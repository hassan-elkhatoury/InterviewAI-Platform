# Download dependencies for both Maven modules.
$ErrorActionPreference = 'Stop'
$maven = Get-Command mvn -ErrorAction Stop
& $maven.Source -f (Join-Path $PSScriptRoot 'pom.xml') dependency:go-offline
exit $LASTEXITCODE
