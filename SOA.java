/* Back-end of Satellite Orbit Analizer: SAO (Main)
 * Used to test the Satellite Class before the development of a GUI
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
  public static void main(String args[]) {
    try {
      long endTime, startTime = System.currentTimeMillis();
      
      // CONFIGURATION
      // The following code is required to configure the Orekit library.
      File orekitData = new File("Libraries/orekit-data"); // The argument indicates the path to the config folder
      DataProvidersManager manager = DataProvidersManager.getInstance();
      manager.addProvider(new DirectoryCrawler(orekitData));
      
      // Set (and create if needed) the output folders
      String outPath = "OutputFolder";
      String sunPath = outPath + "/SunAngles",
        earthPath = outPath + "/EarthAngles",
        accessPath = outPath + "/AccessTimes";
      
      File sunDir = new File(sunPath),
        earthDir = new File(earthPath),
        accessDir = new File(accessPath);
      
      if (!sunDir.exists())
        sunDir.mkdirs(); // Also creates the output folder
      if (!earthDir.exists())
        earthDir.mkdir();
      if (!accessDir.exists())
        accessDir.mkdir();
      
      // GROUND STATION 1
      System.out.println("Setting up ground station 1..."); // Progress indicator
      double longitude = Math.toRadians(7.84965); // Freiburg longitude
      double latitude = Math.toRadians(47.6652); // Freiburg latitude
      double altitude = 325.036; // [m]??? 0.325036 km
      GeodeticPoint stationFreiburg = new GeodeticPoint(latitude, longitude, altitude); // Create location point
      // Topocentric frame for the ground station.
      Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
      BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                             Constants.WGS84_EARTH_FLATTENING,
                                             earthFrame); // Shape of the Earth
      TopocentricFrame stationFrameFreiburg = new TopocentricFrame(earth, stationFreiburg, "StationFreiburg");
      
      // GROUND STATION 2
      System.out.println("Setting up ground station 2..."); // Progress indicator
      longitude = Math.toRadians(-7.84965); // 
      latitude = Math.toRadians(-47.6652); // 
      altitude = 325.036; // [m]??? 0.325036 km
      GeodeticPoint stationUnknown = new GeodeticPoint(latitude, longitude, altitude); // Create location point
      // Topocentric frame for the ground station.
      TopocentricFrame stationFrameUnknown = new TopocentricFrame(earth, stationUnknown, "StationUnknown");
      
      File sta1Dir = new File(accessPath + "/" + stationFrameFreiburg.getName()),
        sta2Dir = new File(accessPath + "/" + stationFrameUnknown.getName());
      
      if (!sta1Dir.exists())
        sta1Dir.mkdir();
      if (!sta2Dir.exists())
        sta2Dir.mkdir();
      
      Satellite sat1 = new Satellite(),
        sat2 = new Satellite();
      
      // Add propagators (random satellites for testing)
      sat1.setTLEPropagator("1 37846U 11060A   18165.13732692 -.00000060  00000-0  00000-0 0  9997",
                            "2 37846  56.1041  61.6028 0004426 354.3829   5.6503  1.70475882 41387");
      sat2.setTLEPropagator("1 33312U 08040A   18155.81913053  .00000031  00000-0  10873-4 0  9991",
                            "2 33312  97.7864 231.7937 0011171 286.7797  73.2178 14.79883098527520");
      
      // Add event detectors
      double maxCheck  = 60.0;
      double threshold =  0.001;
      double elevationDeg = 10.0;
      sat1.setElevationDetector(stationFrameFreiburg, maxCheck, threshold, elevationDeg);
      sat1.setElevationDetector(stationFrameUnknown, maxCheck, threshold, elevationDeg);
      sat2.setElevationDetector(stationFrameFreiburg, maxCheck, threshold, elevationDeg);
      sat2.setElevationDetector(stationFrameUnknown, maxCheck, threshold, elevationDeg);
      
      Thread thread_1 = new Thread(sat1),
        thread_2 = new Thread(sat2);
      
      thread_1.start();
      thread_2.start();
      
      thread_1.join();
      thread_2.join();
      
      endTime = System.currentTimeMillis();
      System.out.println("Done.");
      System.out.println("Elapsed time: " + (endTime - startTime)/1000.0 + " seconds.");
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}