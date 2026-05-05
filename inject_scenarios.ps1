
param (
    [string]$Scenario = "ALL"
)

function Inject-VHAL {
    param($propId, $value)
    Write-Host "Injecting Property ID $propId with value $value"
    adb shell "cmd car_service inject-vhal-event $propId $value"
}

function Run-ECO {
    Write-Host "--- Starting ECO Scenario (Safe Driving) ---"
    for ($i=0; $i -lt 10; $i++) {
        Inject-VHAL 291504647 ($i * 5)  # Speed
        Inject-VHAL 291504901 (1000 + $i * 100) # RPM
        Start-Sleep -Milliseconds 500
    }
}

function Run-AGGRESSIVE {
    Write-Host "--- Starting AGGRESSIVE Scenario (Hard Driving) ---"
    Inject-VHAL 291504647 80 # High Speed
    Inject-VHAL 291504901 5000 # High RPM
    Start-Sleep -Seconds 1
    Inject-VHAL 291505152 1 # Hard Brake
    Inject-VHAL 291504647 10 # Sudden Slowdown
}

if ($Scenario -eq "ALL") {
    Run-ECO
    Start-Sleep -Seconds 2
    Run-AGGRESSIVE
} elseif ($Scenario -eq "ECO") {
    Run-ECO
} elseif ($Scenario -eq "AGGRESSIVE") {
    Run-AGGRESSIVE
}
