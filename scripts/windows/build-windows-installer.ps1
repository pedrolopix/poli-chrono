param(
  [string]$AppName = "PoliChrono",
  [string]$AppVendor = "Lopix Labs",
  [string]$AppVersion = "0.0.0",
  [switch]$VerboseLogging
)

$ErrorActionPreference = "Stop"

function Write-Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "[ERROR] $msg" -ForegroundColor Red }

# Ensure we run from repo root (where mvnw resides)
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path | Split-Path -Parent
Set-Location $repoRoot

# Resolve Maven command (prefer Windows wrapper)
$mvnCmd = if (Test-Path ".\mvnw.cmd") { ".\mvnw.cmd" } elseif (Test-Path "./mvnw") { "./mvnw" } else { "mvn" }

# Resolve version from Maven if not provided
if ($AppVersion -eq "0.0.0") {
  try {
    $AppVersion = (& $mvnCmd -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive org.codehaus.mojo:exec-maven-plugin:3.5.0:exec)
  } catch {
    Write-Warn "Could not resolve version from Maven, using 1.0.0"
    $AppVersion = "1.0.0"
  }
}

Write-Info "Building $AppName $AppVersion (uber-jar)"
& $mvnCmd -B -ntp -Dquarkus.package.jar.type=uber-jar package

# Find the runner jar
$jar = Get-ChildItem -Path target -Filter "*-runner.jar" | Select-Object -First 1
if (-not $jar) {
  Write-Err "Runner JAR not found in target/. Ensure the build succeeded."
  exit 1
}
$jarPath = $jar.FullName
Write-Info "Found runner JAR: $($jar.Name)"

# Tools sanity check
$required = @("jdeps", "jlink", "jpackage", "java")
foreach ($t in $required) {
  $tool = Get-Command $t -ErrorAction SilentlyContinue
  if (-not $tool) { Write-Err "$t not found on PATH. Ensure you are using JDK 21+ (Temurin recommended)."; exit 1 }
}

# Compute required modules using jdeps
Write-Info "Computing required Java modules via jdeps..."
$moduleDeps = & jdeps --ignore-missing-deps --print-module-deps --multi-release 21 --class-path target/lib "${jarPath}" 2>$null
if (-not $moduleDeps) { $moduleDeps = "java.base" }
Write-Info "Modules: $moduleDeps"

# Create custom runtime with jlink
$runtimeDir = Join-Path target "runtime-$AppName"
if (Test-Path $runtimeDir) { Remove-Item -Recurse -Force $runtimeDir }

Write-Info "Creating custom runtime at $runtimeDir"
& jlink --no-header-files --no-man-pages --strip-debug --compress=2 --add-modules "$moduleDeps" --output "$runtimeDir"

# Create application image and installer with jpackage
$appImageDir = Join-Path target "$AppName-image"
if (Test-Path $appImageDir) { Remove-Item -Recurse -Force $appImageDir }

Write-Info "Packaging Windows app image via jpackage"
& jpackage `
  --type app-image `
  --name "$AppName" `
  --vendor "$AppVendor" `
  --app-version "$AppVersion" `
  --input target `
  --main-jar "$($jar.Name)" `
  --runtime-image "$runtimeDir" `
  --dest target `
  --verbose:$VerboseLogging.IsPresent | Out-Null

# Create EXE installer (WiX-free)
Write-Info "Creating Windows EXE installer"
$iconPath = ""
$jpkgArgs = @(
  "--type", "exe",
  "--name", $AppName,
  "--vendor", $AppVendor,
  "--app-version", $AppVersion,
  "--input", "target",
  "--main-jar", $jar.Name,
  "--runtime-image", $runtimeDir,
  "--dest", "target"
)
if ($iconPath -and (Test-Path $iconPath)) { $jpkgArgs += @("--icon", $iconPath) }

& jpackage @jpkgArgs

$exe = Get-ChildItem -Path target -Filter "$AppName-*.exe" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($exe) {
  Write-Info "Installer created: $($exe.FullName)"
  Write-Host "\nNext steps:" -ForegroundColor Green
  Write-Host "  1) Transfer the EXE to a Windows machine." -ForegroundColor Green
  Write-Host "  2) Run the installer and follow prompts. The app includes its own private Java runtime." -ForegroundColor Green
  exit 0
} else {
  Write-Err "Failed to create installer."
  exit 2
}
