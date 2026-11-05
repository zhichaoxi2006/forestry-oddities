Add-Type -AssemblyName System.Drawing

$base = 'D:\dev\ForestryCE\src\main\resources\assets\forestry\textures\item\bees\default'
$outDir = 'D:\dev\Forestry-Oddities\docs\previews'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$outPath = Join-Path $outDir 'zeropoint-bee-tint.png'

# --- load layer frame 0 --------------------------------------------------
function Load-Frame([string]$path) {
    $src = [System.Drawing.Bitmap]::new($path)
    $bmp = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.DrawImage($src, [System.Drawing.Rectangle]::new(0, 0, 16, 16), [System.Drawing.Rectangle]::new(0, 0, 16, 16), [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()
    $src.Dispose()
    return $bmp
}

function Merge-Tinted([System.Drawing.Bitmap]$dst, [System.Drawing.Bitmap]$layer, [int]$tint) {
    $tr = ($tint -shr 16) -band 0xFF
    $tg = ($tint -shr 8) -band 0xFF
    $tb = $tint -band 0xFF
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $s = $layer.GetPixel($x, $y)
            if ($s.A -eq 0) { continue }
            $sr = [int][math]::Round($s.R * $tr / 255.0)
            $sg = [int][math]::Round($s.G * $tg / 255.0)
            $sb = [int][math]::Round($s.B * $tb / 255.0)
            $sa = $s.A / 255.0
            $d = $dst.GetPixel($x, $y)
            $da = $d.A / 255.0
            $oa = $sa + $da * (1.0 - $sa)
            if ($oa -le 0.0001) { $dst.SetPixel($x, $y, [System.Drawing.Color]::Transparent); continue }
            $orr = [int][math]::Round((($sr * $sa) + ($d.R * $da * (1.0 - $sa))) / $oa)
            $ogg = [int][math]::Round((($sg * $sa) + ($d.G * $da * (1.0 - $sa))) / $oa)
            $obb = [int][math]::Round((($sb * $sa) + ($d.B * $da * (1.0 - $sa))) / $oa)
            $orr = [math]::Max(0, [math]::Min(255, $orr))
            $ogg = [math]::Max(0, [math]::Min(255, $ogg))
            $obb = [math]::Max(0, [math]::Min(255, $obb))
            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb([int][math]::Round($oa * 255), $orr, $ogg, $obb))
        }
    }
}

function Compose([string]$stage, [int]$body, [int]$stripes, [int]$outline) {
    $outlineBmp = Load-Frame (Join-Path $base ($stage + '.outline.png'))
    $body1 = Load-Frame (Join-Path $base 'body1.png')
    $stripesBmp = Load-Frame (Join-Path $base 'stripes.png')
    $body2 = Load-Frame (Join-Path $base ($stage + '.body2.png'))
    $c = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    Merge-Tinted $c $outlineBmp $outline
    Merge-Tinted $c $body1 $body
    Merge-Tinted $c $stripesBmp $stripes
    Merge-Tinted $c $body2 0xFFFFFF
    $outlineBmp.Dispose(); $body1.Dispose(); $stripesBmp.Dispose(); $body2.Dispose()
    return $c
}

# --- palette table -------------------------------------------------------
$palettes = @(
    @{ name = '定稿 · 紫罗兰';  body = 0x8A4BE0; stripes = 0xE8FBFF; outline = 0x1A0F2E },
    @{ name = '备选 · 深紫';    body = 0x5B3FA8; stripes = 0xCFEFFF; outline = 0x140B22 },
    @{ name = '已否 · 稿 v1';   body = 0x2E1B5E; stripes = 0xD8FBFF; outline = 0x0B0714 },
    @{ name = '伏打蜂 · 对照';   body = 0xD97C1E; stripes = 0x5FD8FF; outline = 0x2B1608 }
)
$stages = @(
    @{ key = 'queen';    label = '蜂后 Queen' },
    @{ key = 'drone';    label = '雄蜂 Drone' },
    @{ key = 'princess'; label = '公主 Princess' }
)

# --- canvas --------------------------------------------------------------
$W = 1000; $H = 800
$bmp = [System.Drawing.Bitmap]::new($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = 'None'
$g.InterpolationMode = 'NearestNeighbor'
$g.PixelOffsetMode = 'Half'
$g.TextRenderingHint = 'ClearTypeGridFit'
$g.Clear([System.Drawing.Color]::FromArgb(255, 30, 30, 34))

$fontTitle = [System.Drawing.Font]::new('Microsoft YaHei', 19, [System.Drawing.FontStyle]::Bold)
$fontSub = [System.Drawing.Font]::new('Microsoft YaHei', 10.5)
$fontHead = [System.Drawing.Font]::new('Microsoft YaHei', 12, [System.Drawing.FontStyle]::Bold)
$fontSmall = [System.Drawing.Font]::new('Consolas', 9.5)
$fontStage = [System.Drawing.Font]::new('Microsoft YaHei', 12)
$white = [System.Drawing.Brushes]::White
$gray = [System.Drawing.Brushes]::Silver
$dim = [System.Drawing.Brushes]::Gray

$g.DrawString('零点蜂 / Zero-Point Bee — 贴图与 tint 预览', $fontTitle, $white, 20, 14)
$g.DrawString('FCE 的蜜蜂物品 = 4 层 16×16 贴图 + 3 个 tint 颜色；tint 是乘法（图像 = 贴图 × 颜色），所以深色会被压得更深。', $fontSub, $gray, 20, 50)
$g.DrawString('数据来源：assets/forestry/textures/item/bees/default/（queen/drone/princess.outline、body1、stripes、*.body2），取动画第 0 帧。', $fontSub, $dim, 20, 70)

$colX0 = 210; $colW = 190; $iconSize = 160
$headY = 100; $rowY0 = 200; $rowH = 190

for ($i = 0; $i -lt $palettes.Count; $i++) {
    $p = $palettes[$i]
    $x = $colX0 + $i * $colW
    $sw = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, (($p.body -shr 16) -band 0xFF), (($p.body -shr 8) -band 0xFF), ($p.body -band 0xFF)))
    $g.FillRectangle($sw, $x, $headY + 2, 26, 26)
    $g.DrawRectangle([System.Drawing.Pens]::DimGray, $x, $headY + 2, 26, 26)
    $g.DrawString($p.name, $fontHead, $white, ($x + 34), ($headY + 3))
    $g.DrawString(('body  #{0:X6}' -f $p.body), $fontSmall, $gray, $x, ($headY + 34))
    $g.DrawString(('stripes #{0:X6}' -f $p.stripes), $fontSmall, $gray, $x, ($headY + 50))
    $g.DrawString(('outline #{0:X6}' -f $p.outline), $fontSmall, $gray, $x, ($headY + 66))
}

# checkerboard behind icons
for ($ri = 0; $ri -lt $stages.Count; $ri++) {
    $rowY = $rowY0 + $ri * $rowH
    $g.DrawString($stages[$ri].label, $fontStage, $white, 20, ($rowY + 68))
    $g.DrawString('#1E1E22 底', $fontSmall, $dim, 20, ($rowY + 92))
    for ($i = 0; $i -lt $palettes.Count; $i++) {
        $p = $palettes[$i]
        $x = $colX0 + $i * $colW
        $icon = Compose $stages[$ri].key $p.body $p.stripes $p.outline
        $g.DrawImage($icon, [System.Drawing.Rectangle]::new($x, $rowY, $iconSize, $iconSize))
        $g.DrawRectangle([System.Drawing.Pens]::DimGray, $x, $rowY, $iconSize, $iconSize)
        # 1:1 actual size reference
        $g.DrawImage($icon, [System.Drawing.Rectangle]::new(($x + $iconSize + 6), $rowY, 16, 16))
        $icon.Dispose()
    }
}

# raw layers strip
$stripY = 760
$rawLabels = @(
    @{ file = 'queen.outline.png'; label = '描边层' },
    @{ file = 'body1.png';         label = '身体层' },
    @{ file = 'stripes.png';       label = '条纹层' },
    @{ file = 'queen.body2.png';   label = '细节层' }
)
$x = 20
foreach ($r in $rawLabels) {
    $rawBmp = Load-Frame (Join-Path $base $r.file)
    $g.DrawImage($rawBmp, [System.Drawing.Rectangle]::new($x, ($stripY - 40), 48, 48))
    $g.DrawString($r.label, $fontSmall, $gray, $x, ($stripY + 10))
    $rawBmp.Dispose()
    $x += 74
}

$g.Dispose()
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "saved: $outPath"
