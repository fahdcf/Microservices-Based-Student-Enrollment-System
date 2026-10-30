$ErrorActionPreference = 'Stop'
$env:TEST_DATABASES = 'true'
if (-not $env:TEST_DB_PASSWORD) { $env:TEST_DB_PASSWORD = 'local-test-password' }
mvn -B -ntp clean verify
if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed' }
Push-Location "$PSScriptRoot/../frontend/enrollment-app"
try {
    npm ci --no-audit --no-fund
    if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed' }
    npm test -- --watch=false
    if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed' }
    npm run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
} finally { Pop-Location }
