$fn = $preview ? 32 : 256;

bottom=10;
sphereRadius=21;

sizeOfContact = 3.2;
widthOfStabilizer = 2;
heightOfContact = 12;
heightOfStabilizer = 4.5;
zPositionOfContact = -sphereRadius + bottom;

correctSideMarkerWidth = 3.6;
correctSideMarkerDepth = 4.1;
correctSizeMarkerHeight = 10.4;
correctSizeMarkerZPosition = -sphereRadius + bottom;
correctSideMarkerOffset = 14.6;

difference(){
    union(){
        difference(){
            translate([0, 0, -5]) {
              cube([50, 50, 30], center = true);
            }
            translate([0, 0, bottom]) {
              sphere(r = sphereRadius);
            }
            
            // cut near side marker to give the string some space
            
            translate([0, correctSideMarkerOffset * 2 -2, 11.4]) {
                cube([correctSideMarkerWidth, 20, 24], center = true);
            }
        }


        translate([6 + (sizeOfContact / 2),0, zPositionOfContact]) {
            cube([sizeOfContact,sizeOfContact, heightOfContact], center = true);
            
            translate([(widthOfStabilizer), 0, heightOfContact / 2]) {
             cube([widthOfStabilizer, sizeOfContact, heightOfContact + heightOfStabilizer], center = true);    
            }
            
        }

        translate([-6 - (sizeOfContact / 2),0, zPositionOfContact]) {
            cube([sizeOfContact,sizeOfContact,heightOfContact], center = true);
            
            translate([(-widthOfStabilizer), 0, heightOfContact / 2]) {
             cube([widthOfStabilizer, sizeOfContact, heightOfContact + heightOfStabilizer], center = true);    
            }
        }  

        translate([0, correctSideMarkerOffset + (correctSideMarkerDepth / 2), correctSizeMarkerZPosition]) {
            cube([correctSideMarkerWidth,correctSideMarkerDepth, correctSizeMarkerHeight * 2], center = true);
        }
    }

    // Cut the bottom
    translate([0,0,-17.4 -10]) {
        cube([100,100,20], center = true);
    }
    
    // cut wire lines horizontal
    translate([0,0,-15]){
        translate([-58.65,0,0]){
            cube([100,5,5], center = true);
        }
        translate([+58.65,0,0]){
            cube([100,5,5], center = true);
        }
    }
    
    
    pinHeight=8;
    pinBaseDiameter=2;
    pinCorpusDiameter=1.2;
    // cut wire lines vertical
    translate([6 + ((sizeOfContact) / 2),0, zPositionOfContact - 10]) {
        cube([sizeOfContact-1,sizeOfContact-1, 20], center = true);  
    }
    translate([6 + ((sizeOfContact) / 2),0, zPositionOfContact + 2.1]) {
        cylinder(h = pinHeight, d1=pinBaseDiameter, d2=pinCorpusDiameter, center= true); 
    }
    
    translate([-(6 + ((sizeOfContact) / 2)),0, zPositionOfContact - 10]) {
        cube([sizeOfContact-1,sizeOfContact-1, 20], center = true);  
    }
    translate([-(6 + ((sizeOfContact) / 2)),0, zPositionOfContact + 2.1]) {
        cylinder(h = pinHeight, d1=pinBaseDiameter, d2=pinCorpusDiameter, center= true); 
    }
}

