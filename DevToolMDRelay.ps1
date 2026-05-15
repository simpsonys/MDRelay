param(
    [Parameter(Position = 0)]
    [string]$Command
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

$LocalConfigPath = Join-Path $ProjectRoot ".devtool-mdrelay.local.json"
$PromptDir = Join-Path $ProjectRoot ".agent-prompts"
$ScreenshotDir = Join-Path $ProjectRoot ".screenshot"
$DebugApk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"

function Write-Step {
    param([string]$Name)
    Write-Host ""
    Write-Host "==> $Name"
}

function Get-Gradle {
    if (Test-Path ".\gradlew.bat") { return ".\gradlew.bat" }
    if (Test-Path ".\gradlew") { return ".\gradlew" }
    return $null
}

function Invoke-Gradle {
    param([string[]]$Arguments)
    $gradle = Get-Gradle
    if (-not $gradle) { throw "Gradle wrapper not found." }
    Write-Step "$gradle $($Arguments -join ' ')"
    & $gradle @Arguments
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

function Get-Adb {
    $candidates = @()
    if ($env:ANDROID_HOME) { $candidates += Join-Path $env:ANDROID_HOME "platform-tools\adb.exe" }
    if ($env:ANDROID_SDK_ROOT) { $candidates += Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe" }
    $pathAdb = Get-Command adb -ErrorAction SilentlyContinue
    if ($pathAdb) { $candidates += $pathAdb.Source }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) { return $candidate }
    }
    throw "adb not found. Set ANDROID_HOME or ANDROID_SDK_ROOT, or add adb to PATH."
}

function Ensure-GitignoreEntries {
    param([string[]]$Entries)
    $path = Join-Path $ProjectRoot ".gitignore"
    if (-not (Test-Path $path)) { New-Item -ItemType File -Path $path | Out-Null }
    $existing = @(Get-Content $path -ErrorAction SilentlyContinue)
    $changed = $false
    foreach ($entry in $Entries) {
        if ($existing -notcontains $entry) {
            Add-Content -Path $path -Value $entry
            $changed = $true
        }
    }
    if ($changed) { Write-Host "Updated .gitignore" }
}

function Get-PackageName {
    $gradleFiles = @(".\app\build.gradle", ".\app\build.gradle.kts")
    foreach ($file in $gradleFiles) {
        if (Test-Path $file) {
            $text = Get-Content $file -Raw
            if ($text -match "applicationId\s+['""]([^'""]+)['""]") { return $Matches[1] }
            if ($text -match "applicationId\s*=\s*['""]([^'""]+)['""]") { return $Matches[1] }
        }
    }
    Write-Host "Warning: applicationId not found in Gradle files; using fallback package name."
    return "com.simpsonys.mdrelay"
}

function Get-DebugApkPath {
    return $DebugApk
}

function Get-ConnectedDevices {
    $adb = Get-Adb
    $lines = & $adb devices -l
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $devices = @()
    foreach ($line in $lines | Select-Object -Skip 1) {
        if ($line -match "^\s*(\S+)\s+device\s*(.*)$") {
            $serial = $Matches[1]
            $details = $Matches[2]
            $model = ""
            $product = ""
            $device = ""
            if ($details -match "model:(\S+)") { $model = $Matches[1] }
            if ($details -match "product:(\S+)") { $product = $Matches[1] }
            if ($details -match "device:(\S+)") { $device = $Matches[1] }
            $label = if ($model) { $model -replace "_", " " } else { $serial }
            $devices += [pscustomobject]@{
                Serial = $serial
                Label = $label
                Model = $model
                Product = $product
                Device = $device
            }
        }
    }
    return $devices
}

function Show-Devices {
    $devices = @(Get-ConnectedDevices)
    Write-Step "connected devices"
    if ($devices.Count -eq 0) {
        Write-Host "No connected devices."
        return $devices
    }
    for ($i = 0; $i -lt $devices.Count; $i++) {
        $d = $devices[$i]
        $meta = @()
        if ($d.Model) { $meta += "model:$($d.Model)" }
        if ($d.Product) { $meta += "product:$($d.Product)" }
        if ($d.Device) { $meta += "device:$($d.Device)" }
        Write-Host ("{0,2}. {1,-22} {2,-18} {3}" -f ($i + 1), $d.Label, $d.Serial, ($meta -join " "))
    }
    return $devices
}

function Select-Device {
    $devices = Show-Devices
    if ($devices.Count -eq 0) { throw "No connected devices." }
    if ($devices.Count -eq 1) { return $devices[0].Serial }
    $choice = Read-Host "Select device number"
    if ([string]::IsNullOrWhiteSpace($choice)) { throw "Device selection cancelled." }
    $index = [int]$choice - 1
    if ($index -lt 0 -or $index -ge $devices.Count) { throw "Invalid device selection." }
    return $devices[$index].Serial
}

function Select-Devices {
    $devices = Show-Devices
    if ($devices.Count -eq 0) { throw "No connected devices." }
    if ($devices.Count -eq 1) { return @($devices[0].Serial) }
    $choice = Read-Host "Select device numbers, comma-separated, or 'all'"
    if ([string]::IsNullOrWhiteSpace($choice)) { return @() }
    if ($choice.Trim().ToLowerInvariant() -eq "all") { return @($devices.Serial) }
    $serials = @()
    foreach ($part in $choice -split ",") {
        $index = [int]$part.Trim() - 1
        if ($index -lt 0 -or $index -ge $devices.Count) { throw "Invalid device selection: $part" }
        $serials += $devices[$index].Serial
    }
    return $serials
}

function Read-LocalConfig {
    if (-not (Test-Path $LocalConfigPath)) {
        return [pscustomobject]@{ defaultDeviceSerials = @() }
    }
    return Get-Content $LocalConfigPath -Raw | ConvertFrom-Json
}

function Save-DefaultDevices {
    param([string[]]$Serials)
    $config = [pscustomobject]@{ defaultDeviceSerials = @($Serials) }
    $config | ConvertTo-Json -Depth 4 | Set-Content -Path $LocalConfigPath -Encoding UTF8
    Ensure-GitignoreEntries @(".devtool-mdrelay.local.json")
    Write-Host "Saved default devices: $($Serials -join ', ')"
}

function Get-DefaultConnectedSerials {
    $config = Read-LocalConfig
    $saved = @($config.defaultDeviceSerials)
    if ($saved.Count -eq 0) { throw "No default devices saved. Run set-default-devices first." }
    $connected = @(Get-ConnectedDevices)
    $connectedSerials = @($connected.Serial)
    $targets = @()
    foreach ($serial in $saved) {
        if ($connectedSerials -contains $serial) {
            $targets += $serial
        } else {
            Write-Host "Warning: saved device is not connected: $serial"
        }
    }
    if ($targets.Count -eq 0) { throw "No saved default devices are currently connected." }
    return $targets
}

function Ensure-DebugApk {
    if (-not (Test-Path $DebugApk)) {
        Invoke-Build
    }
    if (-not (Test-Path $DebugApk)) { throw "Debug APK not found: $DebugApk" }
}

function Install-ToDevices {
    param([string[]]$Serials)
    Ensure-DebugApk
    Install-ApkToDevices -Serials $Serials -ApkPath $DebugApk -StopOnFailure | Out-Null
}

function Install-ApkToDevices {
    param(
        [string[]]$Serials,
        [string]$ApkPath,
        [switch]$StopOnFailure
    )
    if (-not (Test-Path $ApkPath)) { throw "Debug APK not found: $ApkPath" }
    $adb = Get-Adb
    $installed = @()
    foreach ($serial in $Serials) {
        Write-Step "adb -s $serial install -r $ApkPath"
        & $adb -s $serial install -r $ApkPath | ForEach-Object { Write-Host $_ }
        if ($LASTEXITCODE -eq 0) {
            $installed += $serial
        } elseif ($StopOnFailure) {
            exit $LASTEXITCODE
        } else {
            Write-Host "Warning: install failed on $serial"
        }
    }
    Write-Output $installed
}

function Start-AppOnDevices {
    param(
        [string[]]$Serials,
        [string]$PackageName
    )
    $adb = Get-Adb
    foreach ($serial in $Serials) {
        Write-Step "launch $PackageName on $serial"
        & $adb -s $serial shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1
        if ($LASTEXITCODE -eq 0) { continue }

        Write-Host "Warning: monkey launch failed on $serial; trying resolved launcher activity."
        $resolved = @(& $adb -s $serial shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $PackageName)
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Warning: could not resolve launcher activity for $PackageName on $serial"
            continue
        }

        $activity = $resolved |
            Where-Object { $_ -match "/" -and $_ -notmatch "No activity found" } |
            Select-Object -Last 1
        if ([string]::IsNullOrWhiteSpace($activity)) {
            Write-Host "Warning: no launcher activity found for $PackageName on $serial"
            continue
        }

        Write-Step "adb -s $serial shell am start -n $activity"
        & $adb -s $serial shell am start -n $activity
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Warning: failed to launch $activity on $serial"
        }
    }
}

function Clear-AppDataOnDevices {
    param(
        [string[]]$Serials,
        [string]$PackageName
    )
    $adb = Get-Adb
    foreach ($serial in $Serials) {
        Write-Step "adb -s $serial shell pm clear $PackageName"
        & $adb -s $serial shell pm clear $PackageName
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Warning: could not clear $PackageName on $serial; continuing."
        }
    }
}

function Invoke-Status {
    Write-Step "status"
    git rev-parse --is-inside-work-tree *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Not a git repository."
        return
    }
    Write-Step "git branch"
    git branch --show-current
    Write-Step "git status --short"
    git status --short
    Write-Step "git log --oneline -5"
    git log --oneline -5
}

function Invoke-Build { Invoke-Gradle @("assembleDebug") }
function Invoke-CleanBuild {
    Invoke-Gradle @("--stop")
    Invoke-Gradle @("clean", "assembleDebug")
}
function Invoke-Test { Invoke-Gradle @("testDebugUnitTest") }
function Invoke-Lint { Invoke-Gradle @("lintDebug") }
function Invoke-Check {
    Invoke-Build
    Invoke-Test
    Invoke-Lint
}

function Invoke-Devices { Show-Devices | Out-Null }

function Invoke-SetDefaultDevices {
    $serials = Select-Devices
    if ($serials.Count -eq 0) {
        Write-Host "No changes made."
        return
    }
    Save-DefaultDevices $serials
}

function Invoke-InstallDebug {
    $serials = Select-Devices
    if ($serials.Count -eq 0) { throw "Install cancelled." }
    Install-ToDevices $serials
}

function Invoke-InstallDefault {
    Install-ToDevices (Get-DefaultConnectedSerials)
}

function Invoke-InstallAll {
    $devices = @(Get-ConnectedDevices)
    if ($devices.Count -eq 0) { throw "No connected devices." }
    Install-ToDevices @($devices.Serial)
}

function Invoke-Run {
    $packageName = Get-PackageName
    $serials = Select-Devices
    if ($serials.Count -eq 0) { throw "Run cancelled." }
    Start-AppOnDevices -Serials $serials -PackageName $packageName
}

function Invoke-InstallRun {
    $packageName = Get-PackageName
    $serials = Select-Devices
    if ($serials.Count -eq 0) { throw "Install/run cancelled." }
    Invoke-Build
    $apk = Get-DebugApkPath
    Ensure-DebugApk
    $installed = @(Install-ApkToDevices -Serials $serials -ApkPath $apk)
    if ($installed.Count -eq 0) { throw "Install failed on all selected devices." }
    Start-AppOnDevices -Serials $installed -PackageName $packageName
}

function Invoke-CleanInstallRun {
    $packageName = Get-PackageName
    $serials = Select-Devices
    if ($serials.Count -eq 0) { throw "Clean install/run cancelled." }
    Clear-AppDataOnDevices -Serials $serials -PackageName $packageName
    Invoke-CleanBuild
    $apk = Get-DebugApkPath
    Ensure-DebugApk
    $installed = @(Install-ApkToDevices -Serials $serials -ApkPath $apk)
    if ($installed.Count -eq 0) { throw "Install failed on all selected devices." }
    Start-AppOnDevices -Serials $installed -PackageName $packageName
}

function Invoke-Logcat {
    $serial = Select-Device
    $adb = Get-Adb
    $packageName = Get-PackageName
    Write-Step "adb -s $serial logcat"
    if ($packageName) {
        Write-Host "Package hint: $packageName"
    }
    & $adb -s $serial logcat
}

function Invoke-ScreenshotSetup {
    New-Item -ItemType Directory -Force -Path $ScreenshotDir | Out-Null
    Ensure-GitignoreEntries @(".screenshot/", "screenshot_*.png", "Screenshot_*.png")
    Write-Host "Screenshot folder: $ScreenshotDir"
}

function Get-SafeDeviceName {
    param([string]$Serial)
    $device = @(Get-ConnectedDevices) | Where-Object { $_.Serial -eq $Serial } | Select-Object -First 1
    $name = if ($device -and $device.Model) { $device.Model } else { $Serial }
    return ($name -replace "[^A-Za-z0-9_.-]", "_")
}

function Invoke-Screenshot {
    Invoke-ScreenshotSetup
    $serial = Select-Device
    $adb = Get-Adb
    $stamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $safeName = Get-SafeDeviceName $serial
    $target = Join-Path $ScreenshotDir "$stamp`_$safeName.png"

    Write-Step "screencap $serial"
    $cmd = '"' + $adb + '" -s "' + $serial + '" exec-out screencap -p > "' + $target + '"'
    cmd /c $cmd
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $target) -or (Get-Item $target).Length -eq 0) {
        Write-Host "exec-out failed; trying sdcard fallback."
        $remote = "/sdcard/mdrelay_screenshot.png"
        & $adb -s $serial shell screencap -p $remote
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & $adb -s $serial pull $remote $target
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & $adb -s $serial shell rm $remote
    }
    Write-Host "Saved screenshot: $target"
}

function Open-Folder {
    param([string]$Path)
    Write-Host "Folder: $Path"
    try {
        Start-Process explorer.exe -ArgumentList $Path
    } catch {
        Write-Host "Could not open folder automatically: $($_.Exception.Message)"
    }
}

function Invoke-OpenScreenshotFolder {
    Invoke-ScreenshotSetup
    Open-Folder $ScreenshotDir
}

function Invoke-PromptFolder {
    New-Item -ItemType Directory -Force -Path $PromptDir | Out-Null
    Ensure-GitignoreEntries @(".agent-prompts/")
    Open-Folder $PromptDir
}

function Invoke-CopyLatestPrompt {
    New-Item -ItemType Directory -Force -Path $PromptDir | Out-Null
    Ensure-GitignoreEntries @(".agent-prompts/")
    $latest = Get-ChildItem -Path $PromptDir -Filter "*.md" -File -Recurse |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $latest) {
        Write-Host "No prompt files found under .agent-prompts/."
        Write-Host "Create a .md prompt file there, then run copy-latest-prompt again."
        return
    }
    $content = Get-Content -Path $latest.FullName -Raw -Encoding UTF8
    Set-Clipboard -Value $content
    $lines = if ($content.Length -eq 0) { 0 } else { ($content -split "\r\n|\n|\r").Count }
    $title = ($content -split "\r\n|\n|\r" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -First 1)
    Write-Host "Copied latest prompt to clipboard."
    Write-Host "Path: $($latest.FullName)"
    Write-Host "Modified: $($latest.LastWriteTime)"
    Write-Host "Lines: $lines"
    Write-Host "Characters: $($content.Length)"
    Write-Host "Title: $title"
}

function Show-Help {
    Write-Host @"
MD Relay DevTool

Usage:
  .\DevToolMDRelay.ps1
  .\DevToolMDRelay.ps1 status
  .\DevToolMDRelay.ps1 -Command status

Project:
  status               Repo status + recent commits
  build                Gradle assembleDebug
  clean-build          Gradle clean assembleDebug
  test                 Gradle testDebugUnitTest
  lint                 Gradle lintDebug
  check                build + test + lint

Device:
  devices              List connected Android devices
  set-default-devices  Save default target device serials locally
  install-debug        Install debug APK to selected device(s)
  install-run          Build/install debug APK and launch app
  clean-install-run    Clear app data + clean build/install + launch app
  install-default      Install debug APK to saved default devices
  install-all          Install debug APK to all connected devices
  run                  Launch installed app on selected device(s)
  logcat               Logcat for selected device

Screenshot:
  screenshot-setup     Prepare .screenshot/ and .gitignore
  screenshot           Capture screenshot from selected device
  open-screenshot-folder

Prompts:
  prompt-folder        Ensure/open .agent-prompts/
  open-prompt-folder   Alias for prompt-folder
  copy-latest-prompt   Copy newest .agent-prompts/*.md to clipboard

Examples:
  .\DevToolMDRelay.ps1
  .\DevToolMDRelay.ps1 status
  .\DevToolMDRelay.ps1 check
  .\DevToolMDRelay.ps1 clean-build
  .\DevToolMDRelay.ps1 devices
  .\DevToolMDRelay.ps1 set-default-devices
  .\DevToolMDRelay.ps1 install-run
  .\DevToolMDRelay.ps1 install-default
  .\DevToolMDRelay.ps1 run
  .\DevToolMDRelay.ps1 screenshot
  .\DevToolMDRelay.ps1 copy-latest-prompt
"@
}

function Show-Menu {
    while ($true) {
        try { Clear-Host } catch { }
        Write-Host "============================================================"
        Write-Host "  MD Relay DevTool"
        Write-Host "  $ProjectRoot"
        Write-Host "============================================================"
        Write-Host ""
        Write-Host "[ PROJECT ]"
        Write-Host " P1. [status] Repo status + recent commits"
        Write-Host " P2. [build] Gradle assembleDebug"
        Write-Host " P3. [clean-build] Gradle clean assembleDebug"
        Write-Host " P4. [test] Gradle testDebugUnitTest"
        Write-Host " P5. [lint] Gradle lintDebug"
        Write-Host " P6. [check] build + test + lint"
        Write-Host ""
        Write-Host "[ DEVICE ]"
        Write-Host " D1. [devices] List connected devices"
        Write-Host " D2. [set-default-devices] Select default target devices"
        Write-Host " D3. [install-debug] Install debug APK only"
        Write-Host " D4. [install-run] Build/install debug APK and launch app"
        Write-Host " D5. [clean-install-run] Clear data + clean build/install + launch app"
        Write-Host " D6. [install-default] Install to saved default devices"
        Write-Host " D7. [install-all] Install to all connected devices"
        Write-Host " D8. [run] Launch installed app"
        Write-Host " D9. [logcat] Logcat for selected device"
        Write-Host ""
        Write-Host "[ SCREENSHOT ]"
        Write-Host " S1. [screenshot-setup] Prepare .screenshot/ + .gitignore"
        Write-Host " S2. [screenshot] Capture screenshot from selected device"
        Write-Host " S3. [open-screenshot-folder] Open .screenshot/"
        Write-Host ""
        Write-Host "[ PROMPTS ]"
        Write-Host " 12. [copy-latest-prompt] Copy newest .agent-prompts/*.md to clipboard"
        Write-Host " P0. [prompt-folder] Ensure/open .agent-prompts/"
        Write-Host ""
        Write-Host " H. help"
        Write-Host " Q. quit"
        Write-Host ""
        $choice = Read-Host "Select"
        if ([string]::IsNullOrWhiteSpace($choice)) { continue }
        $normalized = $choice.Trim().ToLowerInvariant()
        if ($normalized -eq "q" -or $normalized -eq "quit") { return }
        try {
            Invoke-CommandName (Resolve-MenuCommand $normalized)
        } catch {
            Write-Host "Error: $($_.Exception.Message)"
        }
        Write-Host ""
        Read-Host "Press Enter to continue" | Out-Null
    }
}

function Resolve-MenuCommand {
    param([string]$Choice)
    switch ($Choice) {
        "p1" { "status" }
        "status" { "status" }
        "p2" { "build" }
        "build" { "build" }
        "p3" { "clean-build" }
        "clean-build" { "clean-build" }
        "p4" { "test" }
        "test" { "test" }
        "p5" { "lint" }
        "lint" { "lint" }
        "p6" { "check" }
        "check" { "check" }
        "d1" { "devices" }
        "devices" { "devices" }
        "d2" { "set-default-devices" }
        "set-default-devices" { "set-default-devices" }
        "d3" { "install-debug" }
        "install-debug" { "install-debug" }
        "d4" { "install-run" }
        "install-run" { "install-run" }
        "d5" { "clean-install-run" }
        "clean-install-run" { "clean-install-run" }
        "d6" { "install-default" }
        "install-default" { "install-default" }
        "d7" { "install-all" }
        "install-all" { "install-all" }
        "d8" { "run" }
        "run" { "run" }
        "d9" { "logcat" }
        "logcat" { "logcat" }
        "s1" { "screenshot-setup" }
        "screenshot-setup" { "screenshot-setup" }
        "s2" { "screenshot" }
        "screenshot" { "screenshot" }
        "s3" { "open-screenshot-folder" }
        "open-screenshot-folder" { "open-screenshot-folder" }
        "12" { "copy-latest-prompt" }
        "copy-latest-prompt" { "copy-latest-prompt" }
        "p0" { "prompt-folder" }
        "prompt-folder" { "prompt-folder" }
        "h" { "help" }
        "help" { "help" }
        default { throw "Unknown menu selection: $Choice" }
    }
}

function Invoke-CommandName {
    param([string]$Name)
    switch ($Name.ToLowerInvariant()) {
        "help" { Show-Help }
        "menu" { Show-Menu }
        "status" { Invoke-Status }
        "build" { Invoke-Build }
        "clean-build" { Invoke-CleanBuild }
        "test" { Invoke-Test }
        "lint" { Invoke-Lint }
        "check" { Invoke-Check }
        "devices" { Invoke-Devices }
        "set-default-devices" { Invoke-SetDefaultDevices }
        "install-debug" { Invoke-InstallDebug }
        "install-run" { Invoke-InstallRun }
        "clean-install-run" { Invoke-CleanInstallRun }
        "install-default" { Invoke-InstallDefault }
        "install-all" { Invoke-InstallAll }
        "run" { Invoke-Run }
        "logcat" { Invoke-Logcat }
        "screenshot-setup" { Invoke-ScreenshotSetup }
        "screenshot" { Invoke-Screenshot }
        "open-screenshot-folder" { Invoke-OpenScreenshotFolder }
        "prompt-folder" { Invoke-PromptFolder }
        "open-prompt-folder" { Invoke-PromptFolder }
        "copy-latest-prompt" { Invoke-CopyLatestPrompt }
        default {
            Write-Host "Unknown command: $Name"
            Show-Help
            exit 2
        }
    }
}

if ([string]::IsNullOrWhiteSpace($Command)) {
    Show-Menu
} else {
    Invoke-CommandName $Command
}
