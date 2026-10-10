// Flat-color raster -> layered Bezier vector paths. Usage: vectorize <input.jpg> <outdir>
import Foundation
import ImageIO
import CoreGraphics
import UniformTypeIdentifiers

setbuf(stdout, nil)
let args = CommandLine.arguments
guard args.count >= 3 else { print("usage: vectorize <input> <outdir>"); exit(1) }
let outDir = args[2]
let simplifyEps = args.count > 3 ? Double(args[3])! : 0.7

// MARK: - Load
func loadRGBA(_ path: String) -> (w: Int, h: Int, px: [UInt8]) {
    let url = URL(fileURLWithPath: path) as CFURL
    guard let src = CGImageSourceCreateWithURL(url, nil),
          let img = CGImageSourceCreateImageAtIndex(src, 0, nil) else { fatalError("cannot load image") }
    let w = img.width, h = img.height
    var px = [UInt8](repeating: 0, count: w * h * 4)
    let cs = CGColorSpace(name: CGColorSpace.sRGB)!
    px.withUnsafeMutableBytes { buf in
        let ctx = CGContext(data: buf.baseAddress, width: w, height: h, bitsPerComponent: 8,
                            bytesPerRow: w * 4, space: cs,
                            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        ctx.draw(img, in: CGRect(x: 0, y: 0, width: w, height: h))
    }
    return (w, h, px)
}

let (W, H, px) = loadRGBA(args[1])
let N = W * H
print("loaded \(W)x\(H)")

// MARK: - Purity (flat-region) test
let purityThreshold = 24
var pure = [Bool](repeating: false, count: N)
for y in 1..<(H - 1) {
    for x in 1..<(W - 1) {
        var mn = (255, 255, 255), mx = (0, 0, 0)
        for dy in -1...1 {
            for dx in -1...1 {
                let i = ((y + dy) * W + (x + dx)) * 4
                let r = Int(px[i]), g = Int(px[i + 1]), b = Int(px[i + 2])
                if r < mn.0 { mn.0 = r }; if g < mn.1 { mn.1 = g }; if b < mn.2 { mn.2 = b }
                if r > mx.0 { mx.0 = r }; if g > mx.1 { mx.1 = g }; if b > mx.2 { mx.2 = b }
            }
        }
        pure[y * W + x] = (mx.0 - mn.0) <= purityThreshold && (mx.1 - mn.1) <= purityThreshold && (mx.2 - mn.2) <= purityThreshold
    }
}

// MARK: - Palette via k-means on pure pixels
// classes: 0 bg, 1 tanLight, 2 tanDark, 3 espresso
var centers: [[Double]] = [[248, 233, 207], [222, 188, 143], [196, 154, 108], [74, 38, 20]]
func nearest(_ r: Int, _ g: Int, _ b: Int, _ c: [[Double]]) -> Int {
    var best = 0, bd = Double.greatestFiniteMagnitude
    for k in 0..<c.count {
        let dr = Double(r) - c[k][0], dg = Double(g) - c[k][1], db = Double(b) - c[k][2]
        let d = dr * dr + dg * dg + db * db
        if d < bd { bd = d; best = k }
    }
    return best
}
for _ in 0..<6 {
    var sum = [[Double]](repeating: [0, 0, 0], count: 4), cnt = [Double](repeating: 0, count: 4)
    var i = 0
    while i < N {
        if pure[i] {
            let k = nearest(Int(px[i * 4]), Int(px[i * 4 + 1]), Int(px[i * 4 + 2]), centers)
            sum[k][0] += Double(px[i * 4]); sum[k][1] += Double(px[i * 4 + 1]); sum[k][2] += Double(px[i * 4 + 2]); cnt[k] += 1
        }
        i += 7
    }
    for k in 0..<4 where cnt[k] > 0 { centers[k] = sum[k].map { $0 / cnt[k] } }
}
print("palette centers:", centers.map { $0.map { Int($0) } })

// MARK: - Label pure pixels, then fill the rest by nearest-pure BFS
var label = [Int8](repeating: -1, count: N)
var queue = [Int32](); queue.reserveCapacity(N)
for i in 0..<N where pure[i] {
    label[i] = Int8(nearest(Int(px[i * 4]), Int(px[i * 4 + 1]), Int(px[i * 4 + 2]), centers))
    queue.append(Int32(i))
}
var head = 0
while head < queue.count {
    let i = Int(queue[head]); head += 1
    let x = i % W, y = i / W
    let l = label[i]
    if x > 0 && label[i - 1] < 0 { label[i - 1] = l; queue.append(Int32(i - 1)) }
    if x < W - 1 && label[i + 1] < 0 { label[i + 1] = l; queue.append(Int32(i + 1)) }
    if y > 0 && label[i - W] < 0 { label[i - W] = l; queue.append(Int32(i - W)) }
    if y < H - 1 && label[i + W] < 0 { label[i + W] = l; queue.append(Int32(i + W)) }
}
for i in 0..<N where label[i] < 0 { label[i] = 0 }

// MARK: - Remove tiny components (4-connected)
let minArea = 60
var comp = [Int32](repeating: -1, count: N)
var nextComp: Int32 = 0
var removed = 0
for s in 0..<N where comp[s] < 0 {
    var stack = [Int32(s)]; comp[s] = nextComp
    var members = [Int32]()
    let l = label[s]
    while let c = stack.popLast() {
        members.append(c)
        let i = Int(c), x = i % W, y = i / W
        if x > 0 && comp[i - 1] < 0 && label[i - 1] == l { comp[i - 1] = nextComp; stack.append(Int32(i - 1)) }
        if x < W - 1 && comp[i + 1] < 0 && label[i + 1] == l { comp[i + 1] = nextComp; stack.append(Int32(i + 1)) }
        if y > 0 && comp[i - W] < 0 && label[i - W] == l { comp[i - W] = nextComp; stack.append(Int32(i - W)) }
        if y < H - 1 && comp[i + W] < 0 && label[i + W] == l { comp[i + W] = nextComp; stack.append(Int32(i + W)) }
    }
    if members.count < minArea {
        var votes = [Int](repeating: 0, count: 4)
        for c in members {
            let i = Int(c), x = i % W, y = i / W
            if x > 0 && label[i - 1] != l { votes[Int(label[i - 1])] += 1 }
            if x < W - 1 && label[i + 1] != l { votes[Int(label[i + 1])] += 1 }
            if y > 0 && label[i - W] != l { votes[Int(label[i - W])] += 1 }
            if y < H - 1 && label[i + W] != l { votes[Int(label[i + W])] += 1 }
        }
        if let best = votes.indices.max(by: { votes[$0] < votes[$1] }), votes[best] > 0 {
            for c in members { label[Int(c)] = Int8(best) }
            removed += 1
        }
    }
    nextComp += 1
}
print("removed \(removed) tiny components")

// MARK: - Debug label image
func writePNG(_ rgba: [UInt8], _ w: Int, _ h: Int, _ path: String) {
    var data = rgba
    let cs = CGColorSpace(name: CGColorSpace.sRGB)!
    data.withUnsafeMutableBytes { buf in
        let ctx = CGContext(data: buf.baseAddress, width: w, height: h, bitsPerComponent: 8, bytesPerRow: w * 4,
                            space: cs, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        let img = ctx.makeImage()!
        let dest = CGImageDestinationCreateWithURL(URL(fileURLWithPath: path) as CFURL, UTType.png.identifier as CFString, 1, nil)!
        CGImageDestinationAddImage(dest, img, nil)
        CGImageDestinationFinalize(dest)
    }
}
let brand: [[UInt8]] = [[0xF6, 0xEC, 0xDD], [0xDC, 0xC7, 0xA9], [0xC7, 0xAE, 0x8A], [0x4E, 0x2F, 0x1B]]
var dbg = [UInt8](repeating: 255, count: N * 4)
for i in 0..<N { let c = brand[Int(label[i])]; dbg[i * 4] = c[0]; dbg[i * 4 + 1] = c[1]; dbg[i * 4 + 2] = c[2] }
writePNG(dbg, W, H, outDir + "/labels.png")

// MARK: - Layer masks (each lower layer is extended 1px under the layers above it, to avoid seams)
func mask(_ classes: Set<Int>) -> [UInt8] {
    var m = [UInt8](repeating: 0, count: N)
    for i in 0..<N where classes.contains(Int(label[i])) { m[i] = 1 }
    return m
}
func extend(_ base: [UInt8], into target: Set<Int>) -> [UInt8] {
    var m = base
    for i in 0..<N where base[i] == 0 && target.contains(Int(label[i])) {
        let x = i % W, y = i / W
        if (x > 0 && base[i - 1] == 1) || (x < W - 1 && base[i + 1] == 1) ||
           (y > 0 && base[i - W] == 1) || (y < H - 1 && base[i + W] == 1) { m[i] = 1 }
    }
    return m
}
let layers: [(name: String, mask: [UInt8])] = [
    ("tanLight", extend(mask([1]), into: [2, 3])),
    ("tanDark", extend(mask([2]), into: [3])),
    ("espresso", mask([3])),
]

// MARK: - Contour tracing (region on the right-hand side of each directed edge)
typealias Pt = (x: Double, y: Double)
func traceLoops(_ m: [UInt8]) -> [[Pt]] {
    let VW = W + 1
    var slotA = [UInt8](repeating: 0, count: VW * (H + 1))
    var slotB = [UInt8](repeating: 0, count: VW * (H + 1))
    func add(_ vx: Int, _ vy: Int, _ dir: Int) {
        let v = vy * VW + vx
        if slotA[v] == 0 { slotA[v] = UInt8(dir + 1) } else { slotB[v] = UInt8(dir + 1) }
    }
    for y in 0..<H {
        for x in 0..<W where m[y * W + x] == 1 {
            if y == 0 || m[(y - 1) * W + x] == 0 { add(x, y, 0) }
            if x == W - 1 || m[y * W + x + 1] == 0 { add(x + 1, y, 1) }
            if y == H - 1 || m[(y + 1) * W + x] == 0 { add(x + 1, y + 1, 2) }
            if x == 0 || m[y * W + x - 1] == 0 { add(x, y + 1, 3) }
        }
    }
    let dx = [1, 0, -1, 0], dy = [0, 1, 0, -1]
    func take(_ v: Int, _ dir: Int) -> Bool {
        if slotA[v] == UInt8(dir + 1) { slotA[v] = slotB[v]; slotB[v] = 0; return true }
        if slotB[v] == UInt8(dir + 1) { slotB[v] = 0; return true }
        return false
    }
    func has(_ v: Int, _ dir: Int) -> Bool { slotA[v] == UInt8(dir + 1) || slotB[v] == UInt8(dir + 1) }
    var loops = [[Pt]]()
    for v0 in 0..<slotA.count where slotA[v0] != 0 {
        while slotA[v0] != 0 {
            var cx = v0 % VW, cy = v0 / VW
            var dir = Int(slotA[v0]) - 1
            var pts = [Pt]()
            var cur = v0
            while true {
                _ = take(cur, dir)
                pts.append((Double(cx), Double(cy)))
                cx += dx[dir]; cy += dy[dir]
                cur = cy * VW + cx
                let order = [(dir + 1) % 4, dir, (dir + 3) % 4]
                var found = -1
                for o in order where has(cur, o) { found = o; break }
                if found < 0 { break }
                dir = found
            }
            loops.append(pts)
        }
    }
    return loops
}

// MARK: - Loop processing
func area(_ p: [Pt]) -> Double {
    var a = 0.0
    for i in 0..<p.count { let j = (i + 1) % p.count; a += p[i].x * p[j].y - p[j].x * p[i].y }
    return a / 2
}
func collapse(_ p: [Pt]) -> [Pt] { // drop collinear vertices
    var out = [Pt]()
    let n = p.count
    for i in 0..<n {
        let a = p[(i + n - 1) % n], b = p[i], c = p[(i + 1) % n]
        if (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x) != 0 { out.append(b) }
    }
    return out
}
func midpoints(_ p: [Pt]) -> [Pt] {
    let n = p.count
    return (0..<n).map { i in let j = (i + 1) % n; return ((p[i].x + p[j].x) / 2, (p[i].y + p[j].y) / 2) }
}
func smooth(_ p: [Pt]) -> [Pt] {
    let n = p.count
    return (0..<n).map { i in
        let a = p[(i + n - 1) % n], b = p[i], c = p[(i + 1) % n]
        return ((a.x + 2 * b.x + c.x) / 4, (a.y + 2 * b.y + c.y) / 4)
    }
}
func distToSeg(_ p: Pt, _ a: Pt, _ b: Pt) -> Double {
    let vx = b.x - a.x, vy = b.y - a.y
    let l2 = vx * vx + vy * vy
    if l2 == 0 { return hypot(p.x - a.x, p.y - a.y) }
    let t = max(0, min(1, ((p.x - a.x) * vx + (p.y - a.y) * vy) / l2))
    return hypot(p.x - (a.x + t * vx), p.y - (a.y + t * vy))
}
func rdp(_ p: [Pt], _ lo: Int, _ hi: Int, _ eps: Double, _ keep: inout [Bool]) {
    var stack = [(lo, hi)]
    while let (l, h) = stack.popLast() {
        if h <= l + 1 { continue }
        var md = 0.0, mi = -1
        for i in (l + 1)..<h { let d = distToSeg(p[i], p[l], p[h]); if d > md { md = d; mi = i } }
        if md > eps { keep[mi] = true; stack.append((l, mi)); stack.append((mi, h)) }
    }
}
func simplifyClosed(_ p: [Pt], _ eps: Double) -> [Pt] {
    let n = p.count
    var far = 0, fd = 0.0
    for i in 1..<n { let d = hypot(p[i].x - p[0].x, p[i].y - p[0].y); if d > fd { fd = d; far = i } }
    var keep = [Bool](repeating: false, count: n + 1)
    let ext = p + [p[0]]
    keep[0] = true; keep[far] = true; keep[n] = true
    rdp(ext, 0, far, eps, &keep)
    rdp(ext, far, n, eps, &keep)
    return (0..<n).filter { keep[$0] }.map { p[$0] }
}
func f(_ v: Double) -> String { String(format: "%.1f", v) }
func bezierPath(_ p: [Pt], cornerDeg: Double) -> String {
    let n = p.count
    func unit(_ a: Pt, _ b: Pt) -> Pt { let l = max(hypot(b.x - a.x, b.y - a.y), 1e-9); return ((b.x - a.x) / l, (b.y - a.y) / l) }
    var tan = [Pt](repeating: (0, 0), count: n)
    var corner = [Bool](repeating: false, count: n)
    for i in 0..<n {
        let a = p[(i + n - 1) % n], b = p[i], c = p[(i + 1) % n]
        let u = unit(a, b), v = unit(b, c)
        let ang = acos(max(-1, min(1, u.x * v.x + u.y * v.y))) * 180 / .pi
        corner[i] = ang > cornerDeg
        let tx = u.x + v.x, ty = u.y + v.y
        let tl = max(hypot(tx, ty), 1e-9)
        tan[i] = (tx / tl, ty / tl)
    }
    var s = "M\(f(p[0].x)),\(f(p[0].y))"
    func segLen(_ a: Int, _ b: Int) -> Double { hypot(p[b].x - p[a].x, p[b].y - p[a].y) }
    for i in 0..<n {
        let j = (i + 1) % n
        let len = segLen(i, j)
        let segDir = unit(p[i], p[j])
        let t0 = corner[i] ? segDir : tan[i]
        let t1 = corner[j] ? segDir : tan[j]
        // Cap each handle by the shorter neighbouring segment so a long straight run next to a
        // gentle turn can't bulge.
        let h0 = corner[i] ? len / 3 : min(len, segLen((i + n - 1) % n, i)) / 3
        let h1 = corner[j] ? len / 3 : min(len, segLen(j, (j + 1) % n)) / 3
        let c1 = (p[i].x + t0.x * h0, p[i].y + t0.y * h0)
        let c2 = (p[j].x - t1.x * h1, p[j].y - t1.y * h1)
        s += "C\(f(c1.0)),\(f(c1.1)) \(f(c2.0)),\(f(c2.1)) \(f(p[j].x)),\(f(p[j].y))"
    }
    return s + "Z"
}

var minX = Double.greatestFiniteMagnitude, minY = minX, maxX = -minX, maxY = -minX
var allAnchors = [Pt]()
var report = ""
for (name, m) in layers {
    var d = ""
    var kept = 0, verts = 0
    for loop in traceLoops(m) {
        if abs(area(loop)) < 40 { continue }
        let c = collapse(loop)
        if c.count < 4 { continue }
        let s = simplifyClosed(smooth(midpoints(c)), simplifyEps)
        if s.count < 3 { continue }
        d += bezierPath(s, cornerDeg: 50)
        kept += 1; verts += s.count
        if name != "tanLight" || true { allAnchors += s }
    }
    try! d.write(toFile: outDir + "/\(name).path", atomically: true, encoding: .utf8)
    report += "\(name): \(kept) loops, \(verts) anchors, \(d.count) chars\n"
}
// visible bounds from the exact (non-extended) classes 1..3
var vb = (minX: Double.greatestFiniteMagnitude, minY: Double.greatestFiniteMagnitude, maxX: -1.0, maxY: -1.0)
for i in 0..<N where label[i] != 0 {
    let x = Double(i % W), y = Double(i / W)
    vb.minX = min(vb.minX, x); vb.maxX = max(vb.maxX, x); vb.minY = min(vb.minY, y); vb.maxY = max(vb.maxY, y)
}
let cx = (vb.minX + vb.maxX) / 2, cy = (vb.minY + vb.maxY) / 2
var rMax = 0.0
for i in 0..<N where label[i] != 0 {
    let x = Double(i % W), y = Double(i / W)
    rMax = max(rMax, hypot(x - cx, y - cy))
}
report += "bounds \(vb.minX) \(vb.minY) \(vb.maxX) \(vb.maxY)\ncenter \(cx) \(cy)\nrMax \(rMax)\n"
try! report.write(toFile: outDir + "/report.txt", atomically: true, encoding: .utf8)
print(report)
