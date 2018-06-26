/* Back-end of Satellite Orbit Analizer: SOA (Main)
 * Used to test the Satellite Class before the development of a GUI
 * 
 * Author: Javier Montemayor
 * Created: 2018-06-15
 * Last update: 2018-06-26
 * 
 * NOTE: Satellite name must NOT contain "/"
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
import java.util.concurrent.*;

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
      double altitude = 325.036; // [m]
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
      altitude = 325.036; // [m]
      GeodeticPoint stationUnknown = new GeodeticPoint(latitude, longitude, altitude); // Create location point
      // Topocentric frame for the ground station.
      TopocentricFrame stationFrameUnknown = new TopocentricFrame(earth, stationUnknown, "StationUnknown");
      
      File sta1Dir = new File(accessPath + "/" + stationFrameFreiburg.getName()),
        sta2Dir = new File(accessPath + "/" + stationFrameUnknown.getName());
      
      if (!sta1Dir.exists())
        sta1Dir.mkdir();
      if (!sta2Dir.exists())
        sta2Dir.mkdir();
      
      String tleData[] = {"GAOFEN 6",
        "1 43484U 18048A   18175.64917903  .00000090  00000-0  20207-4 0  9999",
        "2 43484  98.0511 250.6242 0011130 204.4757 155.5967 14.76872906  3310",
        "LUOJIA-1 01",
        "1 43485U 18048B   18176.19543172  .00000162  00000-0  30774-4 0  9994",
        "2 43485  98.0513 251.1590 0011141 192.8922 167.2009 14.76430878  3394",
        "CZ-2D R_B",
        "1 43486U 18048C   18176.17050062  .00000209  00000-0  36083-4 0  9995",
        "2 43486  97.9294 250.8648 0015615 331.5140  28.5220 14.78039816  3408",
        "CZ-2D DEB",
        "1 43487U 18048D   18175.74559567  .00000300  00000-0  52004-4 0  9995",
        "2 43487  97.9105 250.2877 0034493 178.3278 181.8048 14.74824729  3325",
        "FALCON 9 R_B",
        "1 43489U 18049B   18172.07707761  .00000272  00000-0  95130-4 0  9997",
        "2 43489  26.0812 159.7201 8142749 173.6409 215.6535  1.28104506   227",
        "CZ-2D DEB",
        "1 43490U 18048E   18175.74350433  .00000158  00000-0  31385-4 0  9990",
        "2 43490  98.1787 250.9844 0034951 179.7356 180.3876 14.74953045  3046",
        "CZ-3A R_B",
        "1 43492U 18050B   18175.97003206 -.00000252  00000-0  00000+0 0  9994",
        "2 43492  24.6518  73.7869 7287125 193.6216 123.4239  2.23327886   443",
        "SOYUZ-MS 09",
        "1 43493U 18051A   18175.61067381  .00003886  00000-0  66303-4 0  9999",
        "2 43493  51.6384 341.7075 0003783 223.8232 226.4905 15.53963728  2832",
        "COSMOS 2527 [GLONASS-M]",
        "1 43508U 18053A   18174.20839013  .00000096  00000-0  00000+0 0  9998",
        "2 43508  64.8199 171.4822 0006440 231.7022 228.2698  2.12997363   138",
        "FREGAT R_B",
        "1 43509U 18053B   18175.14346324  .00000091  00000-0  10000-3 0  9991",
        "2 43509  64.8777 171.3753 0090117 122.3110 238.5828  2.09216731   150",
        "REMOVEDEBRIS",
        "1 43510U 98067NT  18176.04402727  .00004239  00000-0  70628-4 0  9997",
        "2 43510  51.6415 339.5336 0003442 223.8147 136.2571 15.54371761   701"
      };
      
      Map<Integer,Satellite> satCollection = new HashMap<Integer,Satellite>();
      
      // Set the propagators
      TimeScale utc = TimeScalesFactory.getUTC();
      AbsoluteDate initialDate = new AbsoluteDate(2021, 01, 01, 00, 00, 00.000, utc);
      
      // Add event detectors
      double maxCheck  = 60.0;
      double threshold =  0.001;
      double elevationDeg = 10.0; // [deg]
      
      int numOfSats = tleData.length/3;
      
      ExecutorService pool = Executors.newFixedThreadPool(numOfSats); // Number of elements in the map
      
      for (int i = 0; i < numOfSats; i++) {
        satCollection.put(i, new Satellite());

        satCollection.get(i).setTLEPropagator(tleData[i*3+1],tleData[i*3+2],tleData[i*3]);
        satCollection.get(i).setAll(initialDate, 10.0*24.0*60.0*60.0, 60.0);
        satCollection.get(i).setElevationDetector(stationFrameFreiburg, maxCheck, threshold, elevationDeg, accessPath);
        satCollection.get(i).setElevationDetector(stationFrameUnknown, maxCheck, threshold, elevationDeg, accessPath);
        satCollection.get(i).setSunPath(sunPath);
        satCollection.get(i).setEarthPath(earthPath);
        
        pool.execute(satCollection.get(i));
      }
      
      pool.shutdown(); // Keeps running current tasks until they finish, disable new tasks from being submitted
      
      System.out.println("Waiting for execution to finish...");
      
      while (!pool.isTerminated()) {
        
      }
      
      endTime = System.currentTimeMillis();
      System.out.println("Done.");
      System.out.println("Elapsed time: " + (endTime - startTime)/1000.0 + " seconds.");
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}