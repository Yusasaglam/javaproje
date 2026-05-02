# JUnit 5 Standalone Console jar'ını indirir.
# PowerShell ile çalıştır: .\lib\junit_indir.ps1

$url  = "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar"
$dest = "$PSScriptRoot\junit-platform-console-standalone-1.10.2.jar"

if (Test-Path $dest) {
    Write-Host "JUnit jar zaten mevcut: $dest"
} else {
    Write-Host "JUnit 5 indiriliyor..."
    Invoke-WebRequest -Uri $url -OutFile $dest
    Write-Host "Tamamlandi: $dest"
}
