import Cocoa
let destination=CommandLine.arguments[1]
let image=NSImage(size:NSSize(width:1024,height:1024))
image.lockFocus()
NSColor(calibratedRed:0.06,green:0.12,blue:0.10,alpha:1).setFill()
NSBezierPath(roundedRect:NSRect(x:32,y:32,width:960,height:960),xRadius:210,yRadius:210).fill()
let green=NSColor(calibratedRed:0.42,green:0.88,blue:0.52,alpha:1)
func line(_ x:CGFloat,_ y:CGFloat,_ x2:CGFloat,_ y2:CGFloat,_ width:CGFloat){green.setStroke();let p=NSBezierPath();p.lineWidth=width;p.lineCapStyle = .round;p.move(to:NSPoint(x:x,y:y));p.line(to:NSPoint(x:x2,y:y2));p.stroke()}
// Friendly robot with one raised arm, drawn as editable vector geometry.
line(385,726,343,801,16);line(577,726,619,801,16)
green.setFill();let head=NSBezierPath();head.appendArc(withCenter:NSPoint(x:481,y:601),radius:190,startAngle:0,endAngle:180,clockwise:false);head.close();head.fill()
NSBezierPath(roundedRect:NSRect(x:291,y:282,width:380,height:289),xRadius:42,yRadius:42).fill()
line(381,293,381,202,67);line(581,293,581,202,67)
line(228,521,228,361,61)
line(729,519,783,609,61);line(783,609,750,721,61)
NSColor(calibratedWhite:0.08,alpha:1).setFill()
for x in [407,555]{NSBezierPath(ovalIn:NSRect(x:x-15,y:653,width:30,height:30)).fill()}
line(830,775,866,806,12);line(867,701,911,707,12)
image.unlockFocus()
let data=image.tiffRepresentation!,bitmap=NSBitmapImageRep(data:data)!
try bitmap.representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:destination))
