[CmdletBinding()]
param(
    [string]$ClasesUrl = "",
    [string]$MiembrosUrl = "",
    [string]$PagosUrl = "",
    [long]$MiembroId = 6,
    [string]$Fecha = "",
    [string]$Salida = ""
)
$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$composeArgs = @("compose", "--project-directory", $repo, "-f", (Join-Path $repo "docker-compose.yml"))
if (-not $Fecha) {
    try { $zona = [TimeZoneInfo]::FindSystemTimeZoneById("America/Bogota") }
    catch { $zona = [TimeZoneInfo]::FindSystemTimeZoneById("SA Pacific Standard Time") }
    $Fecha = [TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow, $zona).AddDays(1).ToString("yyyy-MM-dd")
}
$null = [DateTime]::ParseExact($Fecha, "yyyy-MM-dd", [Globalization.CultureInfo]::InvariantCulture)
if (-not $Salida) { $Salida = Join-Path $repo "docs/evidencias/40-pagos-apagado/ejecucion-docker.json" }
$Salida = [System.IO.Path]::GetFullPath($Salida)
Add-Type -AssemblyName System.Net.Http
# Las URLs corresponden a puertos locales de Docker; no enviarlas a un proxy del sistema.
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.UseProxy = $false
$http = [System.Net.Http.HttpClient]::new($handler)
$http.Timeout = [TimeSpan]::FromSeconds(10)
$detenidoPorPrueba = $false
$fallo = $null
$evidencia = [ordered]@{
    inicio = (Get-Date).ToUniversalTime().ToString("o")
    motor = "Docker Compose + PostgreSQL + RabbitMQ"
    resultado = "PENDIENTE"
    miembroId = $MiembroId
    fecha = $Fecha
    pasos = @()
}
function Invoke-Compose {
    param([string[]]$Argumentos)
    $resultado = & docker @composeArgs @Argumentos
    if ($LASTEXITCODE -ne 0) { throw "Docker Compose fallo: $($Argumentos -join ' ')" }
    return $resultado
}
function Assert-InfraestructuraActiva {
    foreach ($servicio in @("postgres", "rabbitmq", "miembros", "clases")) {
        $id = (Invoke-Compose -Argumentos @("ps", "--status", "running", "-q", $servicio)) -join ""
        if (-not $id.Trim()) { throw "El servicio $servicio debe seguir activo durante la prueba." }
    }
}
function Invoke-Json {
    param([string]$Metodo, [string]$Uri, $Cuerpo = $null)
    $peticion = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Metodo), $Uri)
    $respuesta = $null
    try {
        if ($null -ne $Cuerpo) {
            $json = $Cuerpo | ConvertTo-Json -Depth 12 -Compress
            $peticion.Content = [System.Net.Http.StringContent]::new($json, [System.Text.Encoding]::UTF8, "application/json")
        }
        $respuesta = $http.SendAsync($peticion).GetAwaiter().GetResult()
        $texto = $respuesta.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $objeto = $null
        if ($texto) {
            try { $objeto = $texto | ConvertFrom-Json } catch { $objeto = $null }
        }
        return [PSCustomObject]@{ Status = [int]$respuesta.StatusCode; Body = $objeto; Raw = $texto }
    } finally {
        if ($null -ne $respuesta) { $respuesta.Dispose() }
        $peticion.Dispose()
    }
}
function Wait-Http200 {
    param([string]$Uri)
    $limite = [DateTime]::UtcNow.AddMinutes(2)
    do {
        try {
            $r = Invoke-Json -Metodo "GET" -Uri $Uri
            if ($r.Status -eq 200) { return $r }
        } catch { }
        if ([DateTime]::UtcNow -ge $limite) { throw "El servicio no respondio HTTP 200 dentro del plazo: $Uri" }
        Start-Sleep -Seconds 2
    } while ($true)
}
function Assert-Estado {
    param($Respuesta, [int]$Esperado, [string]$Paso)
    if ($Respuesta.Status -ne $Esperado) { throw "$Paso esperaba HTTP $Esperado y recibio $($Respuesta.Status): $($Respuesta.Raw)" }
    $evidencia.pasos += [ordered]@{ paso = $Paso; status = $Respuesta.Status; respuesta = $Respuesta.Body }
    Write-Host "OK: $Paso (HTTP $Esperado)"
}
try {
    Get-Command docker -ErrorAction Stop | Out-Null
    $servicios = @(Invoke-Compose -Argumentos @("config", "--services"))
    foreach ($servicio in @("clases", "miembros", "pagos")) {
        if ($servicios -notcontains $servicio) { throw "El compose no define $servicio. Actualizar main: la prueba usa el Compose oficial de #41." }
    }
    # Resolver los puertos publicados permite usar .env sin duplicar configuracion.
    foreach ($servicio in @("miembros", "clases", "pagos")) {
        $interno = @{ miembros = 8081; clases = 8082; pagos = 8083 }[$servicio]
        $mapeo = @(Invoke-Compose -Argumentos @("port", $servicio, "$interno"))
        if (-not $mapeo.Count) { throw "No hay puerto publicado para $servicio. Arranca primero la demo." }
        $puerto = ($mapeo[0] -split ":")[-1]
        switch ($servicio) {
            "miembros" { if (-not $MiembrosUrl) { $MiembrosUrl = "http://localhost:$puerto" } }
            "clases" { if (-not $ClasesUrl) { $ClasesUrl = "http://localhost:$puerto" } }
            "pagos" { if (-not $PagosUrl) { $PagosUrl = "http://localhost:$puerto" } }
        }
    }
    $ClasesUrl = $ClasesUrl.TrimEnd("/"); $MiembrosUrl = $MiembrosUrl.TrimEnd("/"); $PagosUrl = $PagosUrl.TrimEnd("/")
    $evidencia.urls = @{ clases = $ClasesUrl; miembros = $MiembrosUrl; pagos = $PagosUrl }
    $evidencia.compose = (Invoke-Compose -Argumentos @("config", "--images"))
    $evidencia.commit = (& git -C $repo rev-parse HEAD 2>$null) -join ""
    $antes = (Invoke-Compose -Argumentos @("ps", "--status", "running", "-q", "pagos")) -join ""
    if (-not $antes.Trim()) { throw "Pagos debe estar en ejecucion ANTES de esta prueba. Arranca los cinco servicios segun la guia." }
    $pago = Wait-Http200 -Uri "$PagosUrl/api/pagos?miembroId=$MiembroId"
    Assert-Estado $pago 200 "Pagos disponible antes de detenerlo"
    $acceso = Wait-Http200 -Uri "$MiembrosUrl/api/miembros/$MiembroId/acceso"
    Assert-Estado $acceso 200 "Consultar miembro antes de detener Pagos"
    if ($acceso.Body.activa -ne $true -or $acceso.Body.plan -ne "PREMIUM") {
        throw "Selecciona un miembro PREMIUM ya activo para evitar limites de plan durante la prueba. El predeterminado es Maria, id=6."
    }
    $catalogo = Wait-Http200 -Uri "$ClasesUrl/api/clases"
    Assert-Estado $catalogo 200 "Catalogo antes de detener Pagos"
    $nueva = @{
        nombre = "Disponibilidad #54 " + (Get-Date).ToString("yyyyMMdd-HHmmss")
        instructor = "Prueba de disponibilidad"
        dias = @("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")
        hora = "18:00"; duracionMinutos = 45; aforo = 10; sala = "Prueba #54"
    }
    $clase = Invoke-Json -Metodo "POST" -Uri "$ClasesUrl/api/clases" -Cuerpo $nueva
    Assert-Estado $clase 201 "Crear clase de prueba independiente"
    $claseId = $clase.Body.id
    $evidencia.claseId = $claseId
    # Pagos estaba corriendo: intentar restaurarlo incluso si stop falla a mitad de camino.
    $detenidoPorPrueba = $true
    Invoke-Compose -Argumentos @("stop", "pagos") | Out-Host
    $despues = (Invoke-Compose -Argumentos @("ps", "--status", "running", "-q", "pagos")) -join ""
    if ($despues.Trim()) { throw "Pagos sigue en ejecucion despues de stop." }
    $contenedor = ((Invoke-Compose -Argumentos @("ps", "-a", "-q", "pagos")) -join "").Trim()
    if (-not $contenedor) { throw "No se encontro el contenedor Pagos detenido." }
    $estadoContenedor = (& docker inspect --format '{{.State.Status}}' $contenedor) -join ""
    if ($LASTEXITCODE -ne 0 -or $estadoContenedor.Trim() -ne "exited") { throw "Pagos no esta en estado exited: $estadoContenedor" }
    $evidencia.pagosEstadoContenedor = $estadoContenedor.Trim()
    $evidencia.pagosContenedorDetenido = $true
    Assert-InfraestructuraActiva
    $respondePagos = $false
    try { $null = Invoke-Json -Metodo "GET" -Uri "$PagosUrl/api/pagos?miembroId=$MiembroId"; $respondePagos = $true } catch { }
    if ($respondePagos) { throw "La URL de Pagos sigue respondiendo; revisar que apunte al contenedor detenido." }
    $evidencia.pagosHttpInaccesible = $true
    $catalogo = Invoke-Json -Metodo "GET" -Uri "$ClasesUrl/api/clases?fecha=$Fecha"
    Assert-Estado $catalogo 200 "Catalogo con Pagos apagado"
    $reserva = Invoke-Json -Metodo "POST" -Uri "$ClasesUrl/api/reservas" -Cuerpo @{ miembroId = $MiembroId; claseId = $claseId; fecha = $Fecha }
    Assert-Estado $reserva 201 "Reservar con miembro activo y Pagos apagado"
    if ($reserva.Body.estado -ne "CONFIRMADA") { throw "La reserva de la clase nueva deberia quedar CONFIRMADA." }
    $evidencia.reservaId = $reserva.Body.id
    if ($reserva.Body.miembroId -ne $MiembroId -or $reserva.Body.claseId -ne $claseId -or $reserva.Body.fecha -ne $Fecha) {
        throw "La reserva no corresponde al miembro, clase y fecha solicitados."
    }
    $historial = Invoke-Json -Metodo "GET" -Uri "$ClasesUrl/api/reservas?miembroId=$MiembroId"
    Assert-Estado $historial 200 "Comprobar reserva persistida"
    if (-not (@($historial.Body) | Where-Object { $_.id -eq $reserva.Body.id -and $_.estado -eq "CONFIRMADA" })) { throw "La reserva no aparece en el historial." }
    $invalida = Invoke-Json -Metodo "POST" -Uri "$ClasesUrl/api/reservas" -Cuerpo @{ miembroId = $null; claseId = $claseId; fecha = $Fecha }
    Assert-Estado $invalida 400 "Peticion invalida produce error controlado"
    if ($invalida.Body.status -ne 400 -or -not $invalida.Body.message -or -not $invalida.Body.timestamp -or -not $invalida.Body.error -or $invalida.Body.path -ne "/api/reservas" -or $invalida.Body.PSObject.Properties.Name -contains "trace") {
        throw "El error no tiene el formato uniforme esperado."
    }
    $catalogo = Invoke-Json -Metodo "GET" -Uri "$ClasesUrl/api/clases?fecha=$Fecha"
    Assert-Estado $catalogo 200 "Catalogo sigue vivo despues del error"
    Assert-InfraestructuraActiva
    $sigueApagado = (Invoke-Compose -Argumentos @("ps", "--status", "running", "-q", "pagos")) -join ""
    if ($sigueApagado.Trim()) { throw "Pagos se reinicio durante la prueba." }
    $evidencia.pagosDetenidoDurantePrueba = $true
    $claseFinal = @(@($catalogo.Body) | Where-Object { $_.id -eq $claseId })
    if ($claseFinal.Count -ne 1 -or $claseFinal[0].cuposLibres -ne 9) { throw "El catalogo no refleja los 9 cupos libres esperados." }
    $evidencia.resultado = "OK"
} catch {
    $fallo = $_
    $evidencia.resultado = "FALLO"
    $evidencia.error = $_.Exception.Message
} finally {
    if ($detenidoPorPrueba) {
        try {
            Invoke-Compose -Argumentos @("start", "pagos") | Out-Host
            $restaurado = Wait-Http200 -Uri "$PagosUrl/api/pagos?miembroId=$MiembroId"
            $evidencia.restauracionPagos = "HTTP 200 tras start"
        } catch {
            $evidencia.restauracionPagos = "FALLO: " + $_.Exception.Message
            $evidencia.resultado = "FALLO"
            if ($null -eq $fallo) { $fallo = $_ }
        }
    }
    $http.Dispose()
    $evidencia.fin = (Get-Date).ToUniversalTime().ToString("o")
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Salida) | Out-Null
    $evidencia | ConvertTo-Json -Depth 20 | Set-Content -Encoding UTF8 -Path $Salida
}
Write-Host "Evidencia guardada: $Salida"
if ($null -ne $fallo) { throw $fallo }
Write-Host "Prueba correcta. La clase y la reserva de prueba se conservan como evidencia."
