$fn = $preview ? 32 : 256;

bottom=10;
sphereRadius=21;

sizeOfContact = 3.2;
widthOfStabilizer = 2;
heightOfContact = 12;
heightOfStabilizer = 3.5;
zPositionOfContact = -sphereRadius + bottom - 0.5;

pinsInnerDistance = 19 / 2;
pinsOuterDistance = 25 / 2;

pinsToleranz = 1;
pinsWidth = 6 - pinsToleranz; // -1 = toleranz


correctSideMarkerWidth = 3.6;
correctSideMarkerDepth = 4.1;
correctSizeMarkerHeight = 10.4;
correctSizeMarkerZPosition = -sphereRadius + bottom;
correctSideMarkerOffset = 14.6;

// debugger
/*
#cube([19,2,6], center = true);
#cube([25,2,3], center = true);

translate([0,0,-6])
#cube([12,2,3], center = true);
*/


difference(){
    union(){
        difference(){
            translate([0, 0, -10]) {
              cube([50, 50, 35], center = true);
            }
            translate([0, 0, bottom]) {
              sphere(r = sphereRadius);
            }
            
            // cut near side marker to give the string some space
            
            translate([0, correctSideMarkerOffset * 2 -2, 11.4]) {
                cube([correctSideMarkerWidth, 20, 24], center = true);
            }
        }
        
        
        translate([pinsInnerDistance - (sizeOfContact / 2),0, zPositionOfContact]) {
            translate([0.2,0,0]){
                cube([sizeOfContact + 0.7,sizeOfContact, heightOfContact], center = true);
            }
            
            translate([pinsWidth / 2 + pinsToleranz / 2 , 0, heightOfContact / 2]) {
                cube([widthOfStabilizer, sizeOfContact, heightOfContact + heightOfStabilizer], center = true);    
            }
            
        }

        translate([-pinsInnerDistance + (sizeOfContact / 2),0, zPositionOfContact]) {
            
            translate([-0.2,0,0]){
                cube([sizeOfContact + 0.7 ,sizeOfContact,heightOfContact], center = true);
            }
            
            translate([-(pinsWidth / 2) - pinsToleranz / 2 , 0, heightOfContact / 2]) {
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
    
    pinHeight=10;
    pinBaseDiameter=2.5;
    pinCorpusDiameter=1.5;
    
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

