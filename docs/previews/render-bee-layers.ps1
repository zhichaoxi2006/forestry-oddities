Add-Type -AssemblyName System.Drawing

$base = 'D:\dev\ForestryCE\src\main\resources\assets\forestry\textures\item\bees\default'
$outPath = 'D:\dev\Forestry-Oddities\docs\previews\bee-layer-map.png'

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
            $s = $layer.GetPixel($x, $y)
            if ($s.A -eq 0) { continue }
            $sa = $s.A / 255.0
            $sr = [int][math]::Round($s.R * $tr / 255.0)
            $sg = [int][math]::Round($s.G * $tg / 255.0)
            $sb = [int][math]::Round($s.B * $tb / 255.0)
            $d = $dst.GetPixel($x, $y)
            $da = $d.A / 255.0
            $oa = $sa + $da * (1.0 - $sa)
            if ($oa -le 0.0001) { $dst.SetPixel($x, $y, [System.Drawing.Color]::Transparent); continue }
            $orr = [int][math]::Round((($sr * $sa) + ($d.R * $da * (1.0 - $sa))) / $oa)
            $ogg = [int][math]::Round((($sg * $sa) + ($d.G * $da * (1.0 - $sa))) / $oa)
            $obb = [int][math]::Round((($sb * $sa) + ($d.B * $da * (1.0 - $sa))) / $oa)
            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb([int][math]::Round($oa * 255), [math]::Min(255, $orr), [math]::Min(255, $ogg), [math]::Min(255, $obb)))
        }
    }
}

function New-Canvas { [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }

$W = 1000; $H = 470
$bmp = [System.Drawing.Bitmap]::new($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = 'None'; $g.InterpolationMode = 'NearestNeighbor'; $g.PixelOffsetMode = 'Half'
$g.Clear([System.Drawing.Color]::FromArgb(255, 30, 30, 34))
$fT = [System.Drawing.Font]::new('Microsoft YaHei', 17, [System.Drawing.FontStyle]::Bold)
$fB = [System.Drawing.Font]::new('Microsoft YaHei', 10.5)
$fL = [System.Drawing.Font]::new('Microsoft YaHei', 11)
$white = [System.Drawing.Brushes]::White; $gray = [System.Drawing.Brushes]::Silver; $dim = [System.Drawing.Brushes]::Gray
$g.DrawString('蜜蜂物品的 4 层与可上色区域（调试着色）', $fT, $white, 20, 12)
$g.DrawString('为了看清"哪一层画了哪块"，把 4 层分别涂成固定色：描边=红、身体=绿、条纹=蓝、细节=白（=不变）。真正能改的是绿 + 蓝两块。', $fB, $gray, 20, 46)

$cells = @(
    @{ label = '描边层 ×红';   kind = 'outline' },
    @{ label = '身体层 ×绿';   kind = 'body1' },
    @{ label = '条纹层 ×蓝';   kind = 'stripes' },
    @{ label = '细节层 ×白';   kind = 'body2' },
    @{ label = '四层叠加';     kind = 'all' }
)

$x0 = 30; $size = 150; $gap = 30; $rowY = 100
for ($i = 0; $i -lt $cells.Count; $i++) {
    $c = $cells[$i]
    $x = $x0 + $i * ($size + $gap)
    $canvas = New-Canvas
    switch ($c.kind) {
        'outline' { Merge-Tinted $canvas (Load-Frame (Join-Path $base 'queen.outline.png')) 0xFF2020 }
        'body1'   { Merge-Tinted $canvas (Load-Frame (Join-Path $base 'body1.png')) 0x20FF20 }
        'stripes' { Merge-Tinted $canvas (Load-Frame (Join-Path $base 'stripes.png')) 0x3050FF }
        'body2'   { Merge-Tinted $canvas (Load-Frame (Join-Path $base 'queen.body2.png')) 0xFFFFFF }
        'all' {
            Merge-Tinted $canvas (Load-Frame (Join-Path $base 'queen.outline.png')) 0xFF2020
            Merge-Tinted $canvas (Load-Frame (Join-Path $base 'body1.png')) 0x20FF20
            Merge-Tinted $canvas (Load-Frame (Join-Path $base 'stripes.png')) 0x3050FF
            Merge-Tinted $canvas (Load-Frame (Join-Path $base 'queen.body2.png')) 0xFFFFFF
        }
    }
    $g.DrawImage($canvas, [System.Drawing.Rectangle]::new($x, $rowY, $size, $size))
    $g.DrawRectangle([System.Drawing.Pens]::DimGray, $x, $rowY, $size, $size)
    $g.DrawString($c.label, $fL, $white, $x, ($rowY + $size + 8))
    $canvas.Dispose()
}

$g.DrawString('结论：翅膀、头、腿、王冠都在"细节层"里，颜色固定（奶油白 + 深褐），任何 species 配色都改不了它们；', $fB, $white, 30, 300)
$g.DrawString('能改的只有腹部底色（body）与腹部的环带（stripes）。所以"深空紫 + 冷白"的形象里，那块奶油色翅膀永远在。', $fB, $white, 30, 326)
$g.DrawString('要连翅膀一起换色，得走 v2 的自定义蜂模型 overlay（IClientRegistration#setCustomBeeModel + 自制 PNG）。', $fB, $dim, 30, 352)

$g.Dispose()
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "saved: $outPath"
