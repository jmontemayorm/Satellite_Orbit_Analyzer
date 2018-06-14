/* Back-end of Satellite Orbit Analizer: SAO (Main)
 * 
 * Author: Javier Montemayor
 * Created: 2018-06-15
 * Last update: 2018-06-15
 * 
 */

// Orekit Libraries
import org.orekit.frames.*;
import org.orekit.time.*;
import org.orekit.data.*;
import org.orekit.orbits.*;
import org.orekit.propagation.*;
import org.orekit.propagation.analytical.*;
import org.orekit.bodies.*;
import org.orekit.utils.*;
import org.orekit.propagation.events.*;
import org.orekit.propagation.events.handlers.*;
import org.orekit.propagation.analytical.tle.*;

// Hipparchus Libraries
import org.hipparchus.geometry.euclidean.threed.*;

// Java Libraries
import java.io.*;
import java.util.*;

// SOA Class
public class SOA {
  
  // Main
  public void main() {
    // This should be a list, stations should be set first, the satellite must be set, ...
    Satellite sat = new Satellite();
  }
}