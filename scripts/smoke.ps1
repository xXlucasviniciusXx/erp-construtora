# Smoke test end-to-end via API (verificação de regressão pós-migrations).
# Uso:  pwsh scripts/smoke.ps1   (backend precisa estar no ar em http://localhost:8080)
# Sai com código != 0 se qualquer verificação falhar.

$ErrorActionPreference = 'Stop'
$base = if ($env:SMOKE_BASE) { $env:SMOKE_BASE } else { 'http://localhost:8080/api' }
$pass = 0; $fail = 0

function Check($name, [scriptblock]$test) {
    try {
        $ok = & $test
        if ($ok) { Write-Host "  [PASS] $name" -ForegroundColor Green; $script:pass++ }
        else     { Write-Host "  [FAIL] $name" -ForegroundColor Red;   $script:fail++ }
    } catch {
        Write-Host "  [FAIL] $name -> $($_.Exception.Message)" -ForegroundColor Red; $script:fail++
    }
}

Write-Host "Smoke test em $base"

# --- Auth ---
$body = '{"email":"admin@construtora.com.br","password":"Admin@123"}'
$login = Invoke-RestMethod -Uri "$base/auth/login" -Method Post -Body ([Text.Encoding]::UTF8.GetBytes($body)) -ContentType 'application/json'
$h = @{ Authorization = "Bearer $($login.token)" }
Check "login retorna token" { $login.token.Length -gt 20 }

# --- Cadastros base ---
$devs = Invoke-RestMethod -Uri "$base/developments" -Headers $h
Check "developments com valores derivados" { $devs.Count -gt 0 -and $null -ne $devs[0].plannedTotal }

$lots = Invoke-RestMethod -Uri "$base/lots" -Headers $h
Check "lots com label hierárquico" { $lots.Count -gt 0 -and $lots[0].label -match '/' }

$cats = Invoke-RestMethod -Uri "$base/categories" -Headers $h
Check "categorias (plano de contas) populadas" { $cats.Count -ge 60 -and $cats[0].grupo }

$ccs = Invoke-RestMethod -Uri "$base/cost-centers" -Headers $h
Check "centros de custo com grupo" { ($ccs | Where-Object { $_.grupo }).Count -gt 0 }

# --- Contas a Pagar: vínculos FK (V10/V11) ---
$pay = Invoke-RestMethod -Uri "$base/accounts-payable?size=200" -Headers $h
Check "contas a pagar trazem categoria/centro/empreendimento" {
    $p = $pay.content | Where-Object { $_.developmentName }
    $p.Count -gt 0 -and ($pay.content | Where-Object { $_.categoryName }).Count -gt 0
}

# --- Parcelas com encargos por atraso ---
$over = Invoke-RestMethod -Uri "$base/installments?status=OVERDUE" -Headers $h
Check "parcelas atrasadas calculam juros/multa (updatedAmount > amount)" {
    $i = $over | Where-Object { $_.daysLate -gt 0 } | Select-Object -First 1
    $i -and ($i.updatedAmount -gt $i.amount)
}

# --- Venda: parcelas geram e somam o financiado ---
$sales = Invoke-RestMethod -Uri "$base/sales" -Headers $h
$saleId = ($sales | Where-Object { $_.installmentsCount -gt 0 } | Select-Object -First 1).id
$sale = Invoke-RestMethod -Uri "$base/sales/$saleId" -Headers $h
Check "parcelas da venda somam (total - entrada)" {
    $somaParcelas = ($sale.installments | Measure-Object -Property amount -Sum).Sum
    $financiado = [math]::Round($sale.totalValue - $sale.downPayment, 2)
    [math]::Abs($somaParcelas - $financiado) -lt 0.01
}

# --- Dashboard analytics: todas as séries novas presentes ---
$an = Invoke-RestMethod -Uri "$base/dashboard/analytics" -Headers $h
Check "dashboard tem séries por empreendimento/categoria/centro" {
    ($null -ne $an.expensesByDevelopment) -and ($null -ne $an.profitByDevelopment) -and
    ($null -ne $an.expensesByCategory) -and ($null -ne $an.expensesByCostCenter)
}

# --- DRE bate com profitByDevelopment (mesma base caixa) ---
$dre = Invoke-RestMethod -Uri "$base/dre" -Headers $h
Check "DRE: resultado = receitas - despesas" {
    [math]::Abs(($dre.totalRevenue - $dre.totalExpense) - $dre.result) -lt 0.01
}

# --- Conciliação ---
$pend = Invoke-RestMethod -Uri "$base/reconciliation/pendencies" -Headers $h
Check "conciliação: pendências acessíveis" { $null -ne $pend }

# --- Relatórios CSV ---
$csv = Invoke-RestMethod -Uri "$base/reports/expenses-by-category" -Headers $h
Check "CSV despesas por categoria com cabeçalho" { $csv -match 'Grupo' -and $csv -match 'Valor Total' }

$dreCsv = Invoke-RestMethod -Uri "$base/dre/export" -Headers $h
Check "CSV do DRE exporta" { $dreCsv -match 'RESULTADO' }

Write-Host ""
Write-Host "Resultado: $pass passaram, $fail falharam" -ForegroundColor $(if ($fail -eq 0) { 'Green' } else { 'Red' })
if ($fail -gt 0) { exit 1 }
