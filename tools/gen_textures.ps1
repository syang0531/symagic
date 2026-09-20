# Generates placeholder staff item textures for SY Magic.
#
#   powershell -ExecutionPolicy Bypass -File tools\gen_textures.ps1
#
# One neutral grayscale staff is drawn from scratch, then tinted once per staff with a colour taken
# from its crafting material (blaze rod = ember orange, packed ice = pale blue, ...). Run a gradle
# task once first so ModDevGradle has produced build/moddev/artifacts (not needed today, but kept so
# the script can pull vanilla templates later without changing shape).
#
# These are placeholders: same silhouette, different hue. Replace with real art when there is any.

Add-Type -AssemblyName System.Drawing

$root  = "$PSScriptRoot\..\src\main\resources\assets\symagic\textures"
$vbase = "$PSScriptRoot\..\build\texture-bases"
New-Item -ItemType Directory -Force -Path "$root\item" | Out-Null
New-Item -ItemType Directory -Force -Path $vbase | Out-Null

# Staff colours - one per staff, read off its crafting material.
$staffColors = @{
  ember_staff    = 'E2762B'   # blaze rod
  frost_staff    = 'A8D8F0'   # packed ice
  lantern_staff  = 'B569D6'   # amethyst shard
  drift_staff    = 'D8D2C0'   # phantom membrane
  bounding_staff = '7BD46B'   # slime ball
  blink_staff    = '2FA88C'   # ender pearl
  tide_staff     = '73B9AE'   # prismarine shard
  healing_staff  = 'EDE7DA'   # ghast tear
  gust_staff     = 'C8E4EC'   # breeze rod
  echo_staff     = '1F6D74'   # echo shard
}

$TINT_BASE = 150.0

function Get-RGB([string]$hex) {
  return @([Convert]::ToInt32($hex.Substring(0,2),16),
           [Convert]::ToInt32($hex.Substring(2,2),16),
           [Convert]::ToInt32($hex.Substring(4,2),16))
}

# Staff base - a neutral grayscale hooked staff (shepherd's crook: a curl at the top-left and a
# shaft descending to the bottom-right). Fully grayscale so the tint recolours all of it; shades sit
# around TINT_BASE so a given material colour lands on the mid tone.
function New-StaffBase([string]$path) {
  $hi=195; $mid=150; $sh=110; $dk=82
  $bmp = New-Object System.Drawing.Bitmap 16,16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(0,0,0,0)) } }
  function Set-Px($x,$y,$v){ if($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16){ $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(255,$v,$v,$v)) } }
  # hook / crook at top-left (open crook, tip curling down)
  Set-Px 4 1 $hi; Set-Px 5 1 $hi; Set-Px 6 1 $mid
  Set-Px 3 2 $hi; Set-Px 7 2 $mid
  Set-Px 3 3 $mid; Set-Px 7 3 $sh
  Set-Px 3 4 $mid; Set-Px 7 4 $sh
  Set-Px 4 4 $dk
  Set-Px 4 5 $sh; Set-Px 5 5 $dk
  # shaft: 2px wide, from the crook base down-right to the foot
  $left  = @(@(7,5),@(8,6),@(8,7),@(9,8),@(9,9),@(10,10),@(10,11),@(11,12),@(11,13),@(12,14))
  $right = @(@(8,5),@(9,6),@(9,7),@(10,8),@(10,9),@(11,10),@(11,11),@(12,12),@(12,13),@(13,14))
  foreach($q in $left)  { Set-Px $q[0] $q[1] $mid }
  foreach($q in $right) { Set-Px $q[0] $q[1] $sh }
  # upper-left highlight edge along the shaft
  foreach($q in @(@(7,5),@(8,6),@(8,7),@(9,8),@(9,9),@(10,10),@(10,11),@(11,12),@(11,13))) { Set-Px $q[0] $q[1] $hi }
  # rounded foot
  Set-Px 12 15 $sh; Set-Px 13 15 $dk
  $bmp.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
}

# Multiply a grayscale template by a colour, keeping its shading.
function Tint-Template([string]$srcPath,[string]$hex,[string]$dstPath) {
  $c = Get-RGB $hex
  $src = New-Object System.Drawing.Bitmap $srcPath
  $dst = New-Object System.Drawing.Bitmap $src.Width,$src.Height
  for($y=0;$y -lt $src.Height;$y++){ for($x=0;$x -lt $src.Width;$x++){
    $p = $src.GetPixel($x,$y)
    if($p.A -eq 0){ $dst.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(0,0,0,0)); continue }
    $lum = ($p.R * 0.299 + $p.G * 0.587 + $p.B * 0.114) / $TINT_BASE
    $r = [Math]::Min(255, [int]($c[0] * $lum))
    $g = [Math]::Min(255, [int]($c[1] * $lum))
    $b = [Math]::Min(255, [int]($c[2] * $lum))
    $dst.SetPixel($x,$y,[System.Drawing.Color]::FromArgb($p.A,$r,$g,$b))
  }}
  $dst.Save($dstPath,[System.Drawing.Imaging.ImageFormat]::Png); $src.Dispose(); $dst.Dispose()
}

# ---------------------------------------------------------------------------
New-StaffBase "$vbase\staff.png"

$count = 0
foreach ($name in $staffColors.Keys) {
  Tint-Template "$vbase\staff.png" $staffColors[$name] "$root\item\$($name).png"
  $count++
}

Write-Output "Wrote $count staff textures to $root\item"
