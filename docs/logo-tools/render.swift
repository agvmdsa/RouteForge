// Render an SVG file to PNG with AppKit. Usage: render <in.svg> <out.png> <size> [bgHex]
import AppKit
import Foundation

let a = CommandLine.arguments
guard a.count >= 4, let size = Int(a[3]) else { print("usage: render in.svg out.png size"); exit(1) }
guard let img = NSImage(contentsOf: URL(fileURLWithPath: a[1])) else { print("cannot load svg"); exit(2) }
let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: size, pixelsHigh: size, bitsPerSample: 8,
                           samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB,
                           bytesPerRow: 0, bitsPerPixel: 0)!
NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
NSColor(calibratedRed: 0.5, green: 0.5, blue: 0.5, alpha: 1).setFill()
NSRect(x: 0, y: 0, width: size, height: size).fill()
img.draw(in: NSRect(x: 0, y: 0, width: size, height: size))
NSGraphicsContext.restoreGraphicsState()
try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: a[2]))
print("ok")
