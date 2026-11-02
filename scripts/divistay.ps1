# DiviStay launcher for Windows: finds Java 21+, downloads Maven once, then runs the site or the tests.
# Used by START-DIVISTAY.bat and TEST-DIVISTAY.bat. Output is also saved to a .log file in the project folder.
param([string]$Mode = "run")
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$project = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $project

function Say($text, $color = "Cyan") { Write-Host ""; Write-Host "  $text" -ForegroundColor $color }

# ---- 1. Java 21 or newer (prefers the lowest version that is >= 21)
function Find-Java {
    $roots = @("$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Java", "$env:ProgramFiles\Microsoft",
               "$env:ProgramFiles\Amazon Corretto", "$env:ProgramFiles\Zulu", "$env:USERPROFILE\.jdks")
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    foreach ($r in $roots) { if (Test-Path -LiteralPath $r) { $candidates += (Get-ChildItem -LiteralPath $r -Directory | ForEach-Object { $_.FullName }) } }
    foreach ($ide in (Get-ChildItem -LiteralPath "$env:ProgramFiles\JetBrains" -Directory -ErrorAction SilentlyContinue)) { $candidates += (Join-Path $ide.FullName "jbr") }
    $best = $null; $bestVersion = 999
    foreach ($c in $candidates) {
        $release = Join-Path $c "release"
        if ((Test-Path -LiteralPath (Join-Path $c "bin\java.exe")) -and (Test-Path -LiteralPath $release)) {
            $m = Select-String -LiteralPath $release -Pattern '^JAVA_VERSION="(\d+)' | Select-Object -First 1
            if ($m) {
                $v = [int]$m.Matches[0].Groups[1].Value
                if ($v -ge 21 -and $v -lt $bestVersion) { $best = $c; $bestVersion = $v }
            }
        }
    }
    if (-not $best) { throw "Java 21 or newer was not found. Install it from https://adoptium.net and try again." }
    Say "Java $bestVersion : $best" "DarkGray"
    return $best
}

# ---- 2. Maven (downloaded once into %LOCALAPPDATA%\DiviStay)
function Get-Maven {
    $version = "3.9.9"
    $mavenHome = Join-Path $env:LOCALAPPDATA "DiviStay\apache-maven-$version"
    $mvn = Join-Path $mavenHome "bin\mvn.cmd"
    if (Test-Path -LiteralPath $mvn) { return $mvn }
    Say "Downloading Maven $version (only the first time)..."
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $zip = Join-Path $env:TEMP "apache-maven-$version-bin.zip"
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.zip"
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    New-Item -ItemType Directory -Force -Path (Join-Path $env:LOCALAPPDATA "DiviStay") | Out-Null
    Expand-Archive -LiteralPath $zip -DestinationPath (Join-Path $env:LOCALAPPDATA "DiviStay") -Force
    Remove-Item -LiteralPath $zip -Force
    return $mvn
}

try {
    $java = Find-Java
    $env:JAVA_HOME = $java
    $env:Path = "$java\bin;$env:Path"
    $mvn = Get-Maven
    # Maven and Java print some warnings to stderr; they must not stop the script
    $ErrorActionPreference = "Continue"

    if ($Mode -eq "test") {
        Say "Running all tests (the first run downloads the libraries, it can take a few minutes)..."
        & $mvn -B test 2>&1 | Tee-Object -FilePath (Join-Path $project "test-output.log")
        if ($LASTEXITCODE -eq 0) { Say "ALL TESTS PASSED" "Green" } else { Say "SOME TESTS FAILED - see test-output.log" "Red" }
    } else {
        $busy = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
        if ($busy) { throw "Port 8080 is already in use. Close the other DiviStay window (or the app using it) and try again." }
        Say "Starting DiviStay... the browser opens by itself when it's ready. Close this window to stop the site."
        # open the browser as soon as the site answers
        Start-Job -ScriptBlock {
            for ($i = 0; $i -lt 300; $i++) {
                try { $c = New-Object Net.Sockets.TcpClient; $c.Connect("localhost", 8080); $c.Close(); Start-Process "http://localhost:8080"; break }
                catch { Start-Sleep -Seconds 1 }
            }
        } | Out-Null
        & $mvn -B spring-boot:run 2>&1 | Tee-Object -FilePath (Join-Path $project "run-output.log")
    }
} catch {
    Say $_.Exception.Message "Red"
}
Write-Host ""
Read-Host "  Press Enter to close"
