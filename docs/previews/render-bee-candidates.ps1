Add-Type -AssemblyName System.Drawing

$base = 'D:\dev\ForestryCE\src\main\resources\assets\forestry\textures\item\bees\default'
$outPath = 'D:\dev\Forestry-Oddities\docs\previews\zeropoint-bee-candidates.png'

function Load-Frame([string]$path) {
    $src = [System.Drawing.Bitmap]::new($path)
    $bmp = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.DrawImage($src, [System.Drawing.Rectangle]::new(0, 0, 16, 16), [System.Drawing.Rectangle]::new(0, 0, 16, 16), [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose(); $src.Dispose()
    return $bmp
}
function Merge-Tinted([System.Drawing.Bitmap]$dst, [System.Drawing.Bitmap]$layer, [int]$tint) {
    $tr = ($tint -shr 16) -band 0xFF; $tg = ($tint -shr 8) -band 0xFF; $tb = $tint -band 0xFF
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $s = $layer.GetPixel($x, $y); if ($s.A -eq 0) { continue }
            $sa = $s.A / 255.0
            $sr = [int][math]::Round($s.R * $tr / 255.0); $sg = [int][math]::Round($s.G * $tg / 255.0); $sb = [int][math]::Round($s.B * $tb / 255.0)
            $d = $dst.GetPixel($x, $y); $da = $d.A / 255.0
            $oa = $sa + $da * (1.0 - $sa); if ($oa -le 0.0001) { continue }
            $orr = [int][math]::Round((($sr * $sa) + ($d.R * $da * (1.0 - $sa))) / $oa)
            $ogg = [int][math]::Round((($sg * $sa) + ($d.G * $da * (1.0 - $sa))) / $oa)
            $obb = [int][math]::Round((($sb * $sa) + ($d.B * $da * (1.0 - $sa))) / $oa)
            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb([int][math]::Round($oa * 255), [math]::Min(255, $orr), [math]::Min(255, $ogg), [math]::Min(255, $obb)))
        }
    }
}
function Compose([string]$stage, [int]$body, [int]$stripes, [int]$outline) {
    $c = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    Merge-Tinted $c (Load-Frame (Join-Path $base ($stage + '.outline.png'))) $outline
    Merge-Tinted $c (Load-Frame (Join-Path $base 'body1.png')) $body
    Merge-Tinted $c (Load-Frame (Join-Path $base 'stripes.png')) $stripes
    Merge-Tinted $c (Load-Frame (Join-Path $base ($stage + '.body2.png'))) 0xFFFFFF
    return $c
}

$cands = @(
    @{ name = 'P0 稿 v1';   body = 0x2E1B5E; stripes = 0xD8FBFF; outline = 0x0B0714 },
    @{ name = 'P1 中紫';    body = 0x7C5CD6; stripes = 0xE8FBFF; outline = 0x1A0F2E },
    @{ name = 'P2 紫罗兰';  body = 0x8A4BE0; stripes = 0xE8FBFF; outline = 0x1A0F2E },
    @{ name = 'P3 深紫';    body = 0x5B3FA8; stripes = 0xCFEFFF; outline = 0x140B22 },
    @{ name = 'P4 靛紫';    body = 0x6A4BD0; stripes = 0xBFE8FF; outline = 0x1A0F2E }
)
$stages = @('queen', 'drone')

$W = 720; $H = 640
$bmp = [System.Drawing.Bitmap]::new($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = 'None'; $g.InterpolationMode = 'NearestNeighbor'; $g.PixelOffsetMode = 'Half'
$g.Clear([System.Drawing.Color]::FromArgb(255, 30, 30, 34))
$fT = [System.Drawing.Font]::new('Microsoft YaHei', 16, [System.Drawing.FontStyle]::Bold)
$fB = [System.Drawing.Font]::new('Microsoft YaHei', 10.5)
$fS = [System.Drawing.Font]::new('Consolas', 9.5)
$white = [System.Drawing.Brushes]::White; $gray = [System.Drawing.Brushes]::Silver; $dim = [System.Drawing.Brushes]::Gray

$g.DrawString('零点蜂：腹部底色候选（谁能在 16×16 里还看得出"紫"）', $fT, $white, 20, 12)
$g.DrawString('FCE 的身体层贴图本身是米褐色（#EDE2D7 / #CEB39B），tint 乘上去会把亮度再压低约 30-40%，所以"深空紫"这类暗色会直接读成黑色。', $fB, $gray, 20, 44)

$colX0 = 40; $cellW = 128; $iconSize = 112; $headY = 80; $rowY0 = 168; $rowH = 150
for ($i = 0; $i -lt $cands.Count; $i++) {
    $p = $cands[$i]; $x = $colX0 + $i * $cellW
    $sw = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, (($p.body -shr 16) -band 0xFF), (($p.body -shr 8) -band 0xFF), ($p.body -band 0xFF)))
    $g.FillRectangle($sw, $x, $headY, 22, 22)
    $g.DrawRectangle([System.Drawing.Pens]::DimGray, $x, $headY, 22, 22)
    $g.DrawString($p.name, $fB, $white, $x, ($headY + 26))
    $g.DrawString(('#{0:X6}' -f $p.body), $fS, $gray, $x, ($headY + 48))
    $g.DrawString(('#{0:X6}' -f $p.stripes), $fS, $dim, $x, ($headY + 64))
}
for ($si = 0; $si -lt $stages.Count; $si++) {
    $rowY = $rowY0 + $si * $rowH
    $label = if ($stages[$si] -eq 'queen') { '蜂后' } else { '雄蜂' }
    $g.DrawString($label, $fB, $white, 8, ($rowY + 46))
    for ($i = 0; $i -lt $cands.Count; $i++) {
        $p = $cands[$i]; $x = $colX0 + $i * $cellW
        $icon = Compose $stages[$si] $p.body $p.stripes $p.outline
        $g.DrawImage($icon, [System.Drawing.Rectangle]::new($x, $rowY, $iconSize, $iconSize))
        $g.DrawRectangle([System.Drawing.Pens]::DimGray, $x, $rowY, $iconSize, $iconSize)
        $g.DrawImage($icon, [System.Drawing.Rectangle]::new(($x + $iconSize + 4), ($rowY + 96), 16, 16))
        $icon.Dispose()
    }
}
$g.DrawString('推荐 P2（紫罗兰 #8A4BE0）：乘完米褐底色后仍是明确的紫罗兰，白条纹读作"电弧"，且与奶油色翅膀不打架。', $fB, $white, 20, 490)
$g.DrawString('P3（深紫）保留"深空"感但偏暗；P0 直接变成"黑蜂"，16×16 下紫的辨识度为零。', $fB, $gray, 20, 516)
$g.DrawString('若以后要真正的"紫到发黑"，只能走自定义模型（v2），而不是继续压 tint。', $fB, $dim, 20, 542)
$g.Dispose()
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "saved: $outPath"
