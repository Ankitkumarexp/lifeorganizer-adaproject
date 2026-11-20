param([switch]$Web)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkCandidates = @()

if ($env:JAVA_HOME) {
    $jdkCandidates += $env:JAVA_HOME
}

$adoptiumRoot = Join-Path $env:ProgramFiles 'Eclipse Adoptium'
if (Test-Path $adoptiumRoot) {
    $jdkCandidates += Get-ChildItem $adoptiumRoot -Directory -Filter 'jdk-25*' | ForEach-Object FullName
}

$jdkHome = $null
foreach ($candidate in $jdkCandidates | Select-Object -Unique) {
    $releaseFile = Join-Path $candidate 'release'
    if (-not (Test-Path $releaseFile)) {
        continue
    }

    $release = Get-Content $releaseFile -Raw
    if ($release -match 'JAVA_VERSION="25(?:\.|")') {
        $jdkHome = $candidate
        break
    }
}

if (-not $jdkHome) {
    throw 'JDK 25 was not found. Install JDK 25 or set JAVA_HOME to its installation folder.'
}

$env:JAVA_HOME = $jdkHome
$env:PATH = "$jdkHome\bin;$env:PATH"

if (-not $env:LIFEORG_DB_USER) {
    $env:LIFEORG_DB_USER = Read-Host 'MySQL username'
}
if (-not $env:LIFEORG_DB_USER) {
    throw 'LIFEORG_DB_USER is required.'
}

if (-not $env:LIFEORG_DB_PASSWORD) {
    $securePassword = Read-Host 'MySQL password' -AsSecureString
    if ($securePassword.Length -eq 0) {
        throw 'LIFEORG_DB_PASSWORD is required.'
    }

    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $env:LIFEORG_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
        $securePassword.Dispose()
    }
}

Set-Location $projectRoot
if ($Web) {
    & mvn spring-boot:run
} else {
    & mvn clean javafx:run
}
exit $LASTEXITCODE
