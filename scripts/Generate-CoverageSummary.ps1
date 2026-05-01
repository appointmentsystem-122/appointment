param(
    [string]$CsvPath = "target/site/jacoco/jacoco.csv",
    [string]$OutputPath = "target/site/jacoco/coverage-summary.md",
    [double]$Threshold = 80
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $CsvPath)) {
    throw "Coverage CSV not found: $CsvPath"
}

$rows = Import-Csv $CsvPath
if (-not $rows -or $rows.Count -eq 0) {
    throw "Coverage CSV is empty: $CsvPath"
}

function Get-Ratio([double]$covered, [double]$missed) {
    $total = $covered + $missed
    if ($total -le 0) { return 100.0 }
    return [math]::Round((100.0 * $covered / $total), 2)
}

function Get-Emoji([double]$value, [double]$threshold) {
    if ($value -ge $threshold) { return "PASS" }
    return "FAIL"
}

$totalLineMiss = ($rows | Measure-Object LINE_MISSED -Sum).Sum
$totalLineCov = ($rows | Measure-Object LINE_COVERED -Sum).Sum
$totalBranchMiss = ($rows | Measure-Object BRANCH_MISSED -Sum).Sum
$totalBranchCov = ($rows | Measure-Object BRANCH_COVERED -Sum).Sum
$totalMethodMiss = ($rows | Measure-Object METHOD_MISSED -Sum).Sum
$totalMethodCov = ($rows | Measure-Object METHOD_COVERED -Sum).Sum

$totalLinePct = Get-Ratio $totalLineCov $totalLineMiss
$totalBranchPct = Get-Ratio $totalBranchCov $totalBranchMiss
$totalMethodPct = Get-Ratio $totalMethodCov $totalMethodMiss

$packageSummary = $rows |
    Group-Object PACKAGE |
    ForEach-Object {
        $pkgRows = $_.Group
        $lineMiss = ($pkgRows | Measure-Object LINE_MISSED -Sum).Sum
        $lineCov = ($pkgRows | Measure-Object LINE_COVERED -Sum).Sum
        $branchMiss = ($pkgRows | Measure-Object BRANCH_MISSED -Sum).Sum
        $branchCov = ($pkgRows | Measure-Object BRANCH_COVERED -Sum).Sum
        $methodMiss = ($pkgRows | Measure-Object METHOD_MISSED -Sum).Sum
        $methodCov = ($pkgRows | Measure-Object METHOD_COVERED -Sum).Sum
        [pscustomobject]@{
            Package = $_.Name
            Line = Get-Ratio $lineCov $lineMiss
            Branch = Get-Ratio $branchCov $branchMiss
            Method = Get-Ratio $methodCov $methodMiss
            Classes = $pkgRows.Count
        }
    } |
    Sort-Object Package

$classSummary = $rows |
    ForEach-Object {
        [pscustomobject]@{
            Package = $_.PACKAGE
            Class = $_.CLASS
            Line = Get-Ratio ([double]$_.LINE_COVERED) ([double]$_.LINE_MISSED)
            Branch = Get-Ratio ([double]$_.BRANCH_COVERED) ([double]$_.BRANCH_MISSED)
            Method = Get-Ratio ([double]$_.METHOD_COVERED) ([double]$_.METHOD_MISSED)
        }
    }

$belowThreshold = $classSummary |
    Where-Object {
        $_.Line -lt $Threshold -or $_.Branch -lt $Threshold -or $_.Method -lt $Threshold
    } |
    Sort-Object Package, Class

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Coverage Summary")
$lines.Add("")
$lines.Add("- Generated at: $timestamp")
$lines.Add("- Threshold: $Threshold%")
$lines.Add("- Source: $CsvPath")
$lines.Add("")
$lines.Add("## Overall")
$lines.Add("")
$lines.Add("| Metric | Coverage | Status |")
$lines.Add("|---|---:|---|")
$lines.Add("| Method | $totalMethodPct% | $(Get-Emoji $totalMethodPct $Threshold) |")
$lines.Add("| Line | $totalLinePct% | $(Get-Emoji $totalLinePct $Threshold) |")
$lines.Add("| Branch | $totalBranchPct% | $(Get-Emoji $totalBranchPct $Threshold) |")
$lines.Add("")
$lines.Add("## By Package")
$lines.Add("")
$lines.Add("| Package | Method | Line | Branch | Classes |")
$lines.Add("|---|---:|---:|---:|---:|")
foreach ($pkg in $packageSummary) {
    $lines.Add("| $($pkg.Package) | $($pkg.Method)% | $($pkg.Line)% | $($pkg.Branch)% | $($pkg.Classes) |")
}
$lines.Add("")
$lines.Add("## Below Threshold ($Threshold%)")
$lines.Add("")
if ($belowThreshold.Count -eq 0) {
    $lines.Add("All classes meet or exceed $Threshold% for Method/Line/Branch.")
} else {
    $lines.Add("| Package | Class | Method % | Line % | Branch % |")
    $lines.Add("|---|---|---:|---:|---:|")
    foreach ($row in $belowThreshold) {
        $lines.Add("| $($row.Package) | $($row.Class) | $($row.Method)% | $($row.Line)% | $($row.Branch)% |")
    }
}

$outDir = Split-Path -Parent $OutputPath
if ($outDir -and -not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir | Out-Null
}

$lines | Set-Content -Path $OutputPath -Encoding UTF8

Write-Output "Coverage summary written to: $OutputPath"
