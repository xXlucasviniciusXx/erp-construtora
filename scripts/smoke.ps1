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

# --- Parcelas: paginação server-side + encargos por atraso ---
$instPage = Invoke-RestMethod -Uri "$base/installments?size=20" -Headers $h
Check "parcelas paginadas (totalPages/totalElements presentes)" {
    ($null -ne $instPage.totalElements) -and ($instPage.content.Count -le 20)
}
$over = Invoke-RestMethod -Uri "$base/installments?status=OVERDUE&size=50" -Headers $h
Check "parcelas atrasadas calculam juros/multa (updatedAmount > amount)" {
    $i = $over.content | Where-Object { $_.daysLate -gt 0 } | Select-Object -First 1
    $i -and ($i.updatedAmount -gt $i.amount)
}

# --- Venda: paginação + parcelas geram e somam o financiado ---
$sales = Invoke-RestMethod -Uri "$base/sales?size=50" -Headers $h
Check "vendas paginadas (totalElements presente)" { $null -ne $sales.totalElements }
$saleId = ($sales.content | Where-Object { $_.installmentsCount -gt 0 } | Select-Object -First 1).id
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

# --- Licenciamento (V13): módulos + licença ---
$lic = Invoke-RestMethod -Uri "$base/licensing/me" -Headers $h
Check "licensing/me traz módulos e licença" {
    ($lic.modules.Count -ge 11) -and ($null -ne $lic.license) -and ($lic.license.plan.Length -gt 0)
}
Check "módulos principais nascem ativos" {
    $dash = $lic.modules | Where-Object { $_.code -eq 'DASHBOARD' } | Select-Object -First 1
    $dash -and $dash.active -eq $true
}

# --- Fase 2: perfis de acesso, permissões por módulo e chave de licenciamento ---
$roles = Invoke-RestMethod -Uri "$base/roles" -Headers $h
Check "perfis de acesso listados (ADMIN protegido)" {
    @($roles | Where-Object { $_.name -eq 'ADMIN' -and $_.system }).Count -eq 1
}
$perms = Invoke-RestMethod -Uri "$base/roles/permissions" -Headers $h
Check "catálogo de permissões por módulo (VIEW/EDIT)" {
    @($perms | Where-Object { $_.code -eq 'CLIENTES_EDIT' -and $_.action -eq 'EDIT' }).Count -eq 1 -and
    @($perms | Where-Object { $_.code -eq 'READ' }).Count -eq 0
}
$genBody = '{"plan":"PROFISSIONAL","periodMonths":12}'
$gen = Invoke-RestMethod -Uri "$base/licensing/license/key/generate" -Method Post -Body ([Text.Encoding]::UTF8.GetBytes($genBody)) -ContentType 'application/json' -Headers $h
Check "gera chave de licenciamento assinada (token HMAC)" { $gen.key -match '\.' -and $gen.key.Length -gt 30 }

# --- V15: Refresh token ---
Check "login retorna refreshToken" { $login.refreshToken.Length -gt 20 }
$refreshBody = "{`"refreshToken`":`"$($login.refreshToken)`"}"
$refreshed = Invoke-RestMethod -Uri "$base/auth/refresh" -Method Post -Body ([Text.Encoding]::UTF8.GetBytes($refreshBody)) -ContentType 'application/json'
Check "refresh emite novo access token e novo refreshToken" {
    $refreshed.token.Length -gt 20 -and $refreshed.refreshToken.Length -gt 20 -and
    $refreshed.refreshToken -ne $login.refreshToken
}
# Usa o novo access token daqui para frente
$h2 = @{ Authorization = "Bearer $($refreshed.token)" }

# --- V15: Listas configuráveis ---
$pm = Invoke-RestMethod -Uri "$base/lists/payment-methods" -Headers $h2
Check "formas de pagamento retornam (min. 4)" { $pm.Count -ge 4 }
$ci = Invoke-RestMethod -Uri "$base/lists/correction-indexes" -Headers $h2
Check "indices de correcao retornam (min. 4)" { $ci.Count -ge 4 }

# --- V18: Cotacoes oficiais do BCB (acumulado 12m) ---
$quotes = Invoke-RestMethod -Uri "$base/lists/correction-indexes/quotes" -Headers $h2
Check "cotacoes BCB: INCC/IGP-M/IPCA tem codigo SGS" {
    @($quotes | Where-Object { $_.sgsCode -ne $null }).Count -ge 3
}
Check "cotacao BCB retorna valor acumulado (ao menos 1 disponivel)" {
    @($quotes | Where-Object { $_.available -eq $true -and $null -ne $_.accumulated12m }).Count -ge 1
}

# --- V17: Categorias de fornecedor + CRUD admin ---
$sc = Invoke-RestMethod -Uri "$base/lists/supplier-categories" -Headers $h2
Check "categorias de fornecedor retornam (min. 5)" { $sc.Count -ge 5 }
$scAll = Invoke-RestMethod -Uri "$base/lists/supplier-categories/all" -Headers $h2
Check "lista admin de categorias de fornecedor (com inativos)" { $scAll.Count -ge 5 }
$newCat = Invoke-RestMethod -Uri "$base/lists/supplier-categories" -Method Post `
    -Body '{"name":"Smoke Teste Cat","active":true,"sortOrder":50}' -ContentType 'application/json' -Headers $h2
Check "cria categoria de fornecedor" { $newCat.id -and $newCat.name -eq 'Smoke Teste Cat' }
Invoke-RestMethod -Uri "$base/lists/supplier-categories/$($newCat.id)" -Method Delete -Headers $h2 | Out-Null
Check "remove categoria de fornecedor criada" { $true }

# --- Cache do dashboard: 2 chamadas retornam a mesma estrutura ---
$an1 = Invoke-RestMethod -Uri "$base/dashboard/analytics" -Headers $h2
$an2 = Invoke-RestMethod -Uri "$base/dashboard/analytics" -Headers $h2
Check "dashboard analytics consistente (cache)" {
    ($null -ne $an1.totalSold) -and ($an1.totalSold -eq $an2.totalSold)
}

# --- DRE: receitas financeiras (linha aparece quando ha juros/multa) ---
$dreCash = Invoke-RestMethod -Uri "$base/dre?basis=CAIXA" -Headers $h2
Check "DRE base caixa retorna resultado coerente" {
    [math]::Abs(($dreCash.totalRevenue - $dreCash.totalExpense) - $dreCash.result) -lt 0.01
}

# --- V15: Reserva de lote ---
# Atribui a variável e indexa [0]: Invoke-RestMethod retorna Object[] que o
# pipeline | Select-Object -First 1 NÃO desempacota corretamente no PS 5.1.
$allDevs = Invoke-RestMethod -Uri "$base/developments" -Headers $h2
$firstDev = $allDevs[0]
if ($firstDev) {
    $devLots = Invoke-RestMethod -Uri "$base/lots?developmentId=$($firstDev.id)" -Headers $h2
    $availLot = $devLots | Where-Object { $_.status -eq 'AVAILABLE' } | Select-Object -First 1
    if ($availLot) {
        $reserved = Invoke-RestMethod -Uri "$base/lots/$($availLot.id)/reserve" -Method Patch `
            -Body '{"hours":1}' -ContentType 'application/json' -Headers $h2
        Check "reserva de lote retorna RESERVED com expiração" {
            $reserved.status -eq 'RESERVED' -and $null -ne $reserved.reservationExpiresAt
        }
        $released = Invoke-RestMethod -Uri "$base/lots/$($reserved.id)/release" -Method Patch -Headers $h2
        Check "liberacao de reserva retorna AVAILABLE" { $released.status -eq 'AVAILABLE' }
    } else {
        Write-Host "  [SKIP] reserva de lote (nenhum lote disponivel no empreendimento)" -ForegroundColor Yellow
    }
} else {
    Write-Host "  [SKIP] reserva de lote (nenhum empreendimento cadastrado)" -ForegroundColor Yellow
}

# --- V19: Contratos (numeração, templates, distrato, arquivo) ---
$salesPage = Invoke-RestMethod -Uri "$base/sales?size=50" -Headers $h2
$someSale = @($salesPage.content)[0]
Check "vendas trazem numero de contrato (CT-NNNNNN)" { $someSale.contractNumber -match '^CT-\d{6}$' }

# Gera contrato PDF e confere arquivamento versionado
$cpdf = Invoke-WebRequest -UseBasicParsing -Uri "$base/contracts/sales/$($someSale.id)/pdf" -Headers $h2
Check "contrato PDF gerado (%PDF)" {
    ($cpdf.Headers['Content-Type'] -match 'pdf') -and ([Text.Encoding]::ASCII.GetString($cpdf.Content[0..3]) -eq '%PDF')
}
$cdocs = @(Invoke-RestMethod -Uri "$base/contracts/sales/$($someSale.id)/documents" -Headers $h2)
Check "documento de contrato arquivado" { @($cdocs | Where-Object { $_.type -eq 'CONTRACT' }).Count -ge 1 }

# Templates: 1 padrao por tipo + preview substitui tokens
$tpls = Invoke-RestMethod -Uri "$base/contract-templates" -Headers $h2
Check "2 modelos padrao (CONTRACT + DISTRATO)" { @($tpls | Where-Object isDefault).Count -eq 2 }
$prev = Invoke-WebRequest -UseBasicParsing -Uri "$base/contract-templates/preview" -Method Post -Headers $h2 `
    -Body ([Text.Encoding]::UTF8.GetBytes('{"body":"<x>{{cliente_nome}}</x>"}')) -ContentType 'application/json'
Check "preview de modelo substitui tokens" { "$($prev.Content)" -match 'Jo.o da Silva' }

# Fluxo de distrato: cria venda -> distrata -> libera lote -> remove (cleanup)
$cli = @((Invoke-RestMethod -Uri "$base/clients?size=1" -Headers $h2).content)[0]
$lotsAll = Invoke-RestMethod -Uri "$base/lots" -Headers $h2
$freeLot = @($lotsAll | Where-Object { $_.status -eq 'AVAILABLE' })[0]
if ($cli -and $freeLot) {
    $saleJson = @{ clientId=$cli.id; lotId=$freeLot.id; totalValue=120000; downPayment=0; installmentsCount=6;
        firstDueDate='2026-09-10'; purchaseType='Financiamento próprio'; paymentMethod='Boleto'; correctionIndex='Sem correção' } | ConvertTo-Json
    $newSale = Invoke-RestMethod -Uri "$base/sales" -Method Post -Headers $h2 -Body ([Text.Encoding]::UTF8.GetBytes($saleJson)) -ContentType 'application/json'
    Check "venda criada recebe contractNumber sequencial" { $newSale.contractNumber -match '^CT-\d{6}$' }
    $distJson = @{ distratoDate='2026-06-05'; reason='Smoke distrato'; refundAmount=0; retainedAmount=0 } | ConvertTo-Json
    $distd = Invoke-RestMethod -Uri "$base/sales/$($newSale.id)/distrato" -Method Post -Headers $h2 -Body ([Text.Encoding]::UTF8.GetBytes($distJson)) -ContentType 'application/json'
    Check "distrato marca CANCELLED e grava data" { $distd.status -eq 'CANCELLED' -and $null -ne $distd.distratoDate }
    Check "lote liberado apos distrato" { (Invoke-RestMethod -Uri "$base/lots/$($freeLot.id)" -Headers $h2).status -eq 'AVAILABLE' }
    $dpdf = Invoke-WebRequest -UseBasicParsing -Uri "$base/contracts/sales/$($newSale.id)/distrato/pdf" -Headers $h2
    Check "distrato PDF gerado (%PDF)" { [Text.Encoding]::ASCII.GetString($dpdf.Content[0..3]) -eq '%PDF' }
    Invoke-RestMethod -Uri "$base/sales/$($newSale.id)" -Method Delete -Headers $h2 | Out-Null
} else {
    Write-Host "  [SKIP] fluxo de distrato (sem cliente ou lote disponivel)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Resultado: $pass passaram, $fail falharam" -ForegroundColor $(if ($fail -eq 0) { 'Green' } else { 'Red' })
if ($fail -gt 0) { exit 1 }
