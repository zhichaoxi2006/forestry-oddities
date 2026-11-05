Add-Type -AssemblyName System.Drawing

$base = 'D:\dev\ForestryCE\src\main\resources\assets\forestry\textures\item\bees\default'
$outPath = 'D:\dev\Forestry-Oddities\docs\previews\voltaic-bee-tint.png'

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
    @{ name = 'V0 稿 v1';      body = 0xD97C1E; stripes = 0x9BE8FF; outline = 0x2B1608 },
    @{ name = 'V1 铜更浓';     body = 0xC16A1C; stripes = 0x9BE8FF; outline = 0x2B1608 },
    @{ name = 'V2 更亮橙';     body = 0xF0912A; stripes = 0xB8F2FF; outline = 0x3A1A06 },
    @{ name = 'V3 电弧更青';   body = 0xD97C1E; stripes = 0x5FD8FF; outline = 0x2B1608 },
    @{ name = 'V4 描边近黑';   body = 0xD97C1E; stripes = 0x9BE8FF; outline = 0x120A04 }
)
$stages = @(
    @{ key = 'queen';    label = '蜂后 Queen' },
    @{ key = 'drone';    label = '雄蜂 Drone' },
    @{ key = 'princess'; label = '公主 Princess' }
)

$W = 1180; $H = 820
$bmp = [System.Drawing.Bitmap]::new($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = 'None'; $g.InterpolationMode = 'NearestNeighbor'; $g.PixelOffsetMode = 'Half'
$g.TextRenderingHint = 'ClearTypeGridFit'
$g.Clear([System.Drawing.Color]::FromArgb(255, 30, 30, 34))

$fT = [System.Drawing.Font]::new('Microsoft YaHei', 19, [System.Drawing.FontStyle]::Bold)
$fB = [System.Drawing.Font]::new('Microsoft YaHei', 10.5)
$fH = [System.Drawing.Font]::new('Microsoft YaHei', 12, [System.Drawing.FontStyle]::Bold)
$fS = [System.Drawing.Font]::new('Consolas', 9.5)
$fL = [System.Drawing.Font]::new('Microsoft YaHei', 12)
$white = [System.Drawing.Brushes]::White; $gray = [System.Drawing.Brushes]::Silver; $dim = [System.Drawing.Brushes]::Gray

$g.DrawString('伏打蜂 / Voltaic Bee — 贴图与 tint 预览', $fT, $white, 20, 14)
$g.DrawString('同一套 FCE 蜜蜂贴图，只换 body / stripes / outline 三个数字。tint 是乘法，所以橙色会被米褐底色压成"铜"而不是"亮橙"。', $fB, $gray, 20, 50)
$g.DrawString('数据来源：assets/forestry/textures/item/bees/default/，动画第 0 帧；渲染方式见 docs/previews/render-voltaic-tint.ps1。', $fB, $dim, 20, 70)

$colX0 = 200; $colW = 194; $iconSize = 160; $headY = 100; $rowY0 = 212; $rowH = 194
for ($i = 0; $i -lt $cands.Count; $i++) {
    $p = $cands[$i]; $x = $colX0 + $i * $colW
    $sw = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, (($p.body -shr 16) -band 0xFF), (($p.body -shr 8) -band 0xFF), ($p.body -band 0xFF)))
    $g.FillRectangle($sw, $x, $headY, 24, 24)
    $g.DrawRectangle([System.Drawing.Pens]::DimGray, $x, $headY, 24, 24)
    $g.DrawString($p.name, $fH, $white, ($x + 32), ($headY + 2))
    $g.DrawString(('body  #{0:X6}' -f $p.body), $fS, $gray, $x, ($headY + 32))
    $g.DrawString(('stripe #{0:X6}' -f $p.stripes), $fS, $gray, $x, ($headY + 48))
    $g.DrawString(('outlin #{0:X6}' -f $p.outline), $fS, $gray, $x, ($headY + 64))
}
for ($si = 0; $si -lt $stages.Count; $si++) {
    $rowY = $rowY0 + $si * $rowH
    $g.DrawString($stages[$si].label, $fL, $white, 16, ($rowY + 64))
    for ($i = 0; $i -lt $cands.Count; $i++) {
        $p = $cands[$i]; $x = $colX0 + $i * $colW
        $icon = Compose $stages[$si].key $p.body $p.stripes $p.outline
        $g.DrawImage($icon, [System.Drawing.Rectangle]::new($x, $rowY, $iconSize, $iconSize))
        $g.DrawRectangle([System.Drawing.Pens]::DimGray, $x, $rowY, $iconSize, $iconSize)
        $g.DrawImage($icon, [System.Drawing.Rectangle]::new(($x + $iconSize + 6), $rowY, 16, 16))
        $icon.Dispose()
    }
}
$g.Dispose()
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "saved: $outPath"
