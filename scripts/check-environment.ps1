Write-Host 'Java:'
java -version
Write-Host 'Maven:'
mvn -version
Write-Host 'MySQL service candidates:'
Get-Service | Where-Object { $_.Name -match 'mysql' -or $_.DisplayName -match 'mysql' } | Format-Table -AutoSize
