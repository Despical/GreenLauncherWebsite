$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    $localJdk = Join-Path $env:USERPROFILE '.jdks/openjdk-25'
    if (Test-Path -LiteralPath (Join-Path $localJdk 'bin/java.exe')) { $env:JAVA_HOME = $localJdk }
    else { throw 'JDK 25 gerekli. JAVA_HOME değişkenini JDK 25 klasörüne ayarla.' }
}
Write-Host 'Green Launcher Web: http://localhost:8087'
Write-Host 'Yönetim: http://localhost:8087/admin'
Write-Host 'Yerel admin parolası ilk açılışta data/local-admin-password.txt dosyasında oluşturulur.'
& .\gradlew.bat bootRun --console=plain
exit $LASTEXITCODE
