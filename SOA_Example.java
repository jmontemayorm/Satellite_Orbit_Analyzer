/* This file is meant to demonstrate how to use the Orekit library to setup an orbit, an event handler, and propagate
 * the orbit to obtain data about the orientation and position of the satellite as well as access times to a ground
 * station.
 */

// The following orekit libraries are used in this example, make sure the libraries are added as extra classpaths in
// the IDE.
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

// The following hipparchus library is used in this example, the library must be added like with orekit.
import org.hipparchus.geometry.euclidean.threed.*;

// The following java libraries are used in this example, no need to add them manually to the IDE.
import java.io.*;
import java.util.*;

// Beginning of the class
public class SOA_Example {
  // The following class variables are declared here so that they can be accessed from the main method and the event
  // handler detecting access times.
  public static PrintWriter accessTimes;
  public static int accessNum = 1;
  public static AbsoluteDate accessBegin, accessEnd;
  public static Locale loc = new Locale("EN","US");
  
  // EventHandler
  private static class VisibilityHandler implements EventHandler<ElevationDetector> {
    public Action eventOccurred(final SpacecraftState s, final ElevationDetector detector,
                                final boolean increasing) {
      if (increasing) {
        // Satellite entering access zone, get date-time into class variable to compare later.
        accessBegin = s.getDate();
        
        return Action.CONTINUE;
      } else {
        // Satellite is exiting access zone, get date-time.
        accessEnd = s.getDate();
        
        try{
          // Extract date, time and duration of the access.
          double visDuration = accessEnd.durationFrom(accessBegin);
          DateTimeComponents compsBegin = accessBegin.getComponents(0); // Argument 0 means synched with UTC
          DateTimeComponents compsEnd = accessEnd.getComponents(0); // Argument 0 means synched with UTC
          DateComponents dateBegin = compsBegin.getDate();
          DateComponents dateEnd = compsEnd.getDate();
          TimeComponents timeBegin = compsBegin.getTime();
          TimeComponents timeEnd = compsEnd.getTime();
          
          // Write access into file.
          accessTimes.printf(loc,"%d,%d %s %d %02d:%02d:%06.3f,%d %s %d %02d:%02d:%06.3f,%07.3f\n",accessNum++,
                             dateBegin.getDay(),dateBegin.getMonthEnum().getCapitalizedAbbreviation(),
                             dateBegin.getYear(),timeBegin.getHour(),timeBegin.getMinute(),timeBegin.getSecond(),
                             dateEnd.getDay(),dateEnd.getMonthEnum().getCapitalizedAbbreviation(),dateEnd.getYear(),
                             timeEnd.getHour(),timeEnd.getMinute(),timeEnd.getSecond(),visDuration);
        } catch (Exception ex) {
          // Notify in case or error.
          System.out.println("Error calculating or writing to Access Times file.\n" + ex);
        }
        
        return Action.STOP;
      }
    }
  }
  
  // Main method
  public static void main(String args[]) {
    // The following variables are used to measure the time it takes to run the code.
    long endTime, startTime = System.currentTimeMillis();
    
    try {
      // CONFIGURATION
      // The following code is required to configure the Orekit library.
      File orekitData = new File("Libraries/orekit-data"); // The argument indicates the path to the config folder
      DataProvidersManager manager = DataProvidersManager.getInstance();
      manager.addProvider(new DirectoryCrawler(orekitData));
      
      // INERTIAL FRAME
      // An Earth centered inertial frame is created.
      System.out.println("Setting up inertial frame..."); // Progress indicator
      Frame inertialFrame = FramesFactory.getEME2000();
      
      // INITIAL STATE
      // Setup the starting date-time with respect to UTC.
      System.out.println("Setting up initial state..."); // Progress indicator
      TimeScale utc = TimeScalesFactory.getUTC();
      AbsoluteDate initialDate = new AbsoluteDate(2021, 01, 01, 00, 00, 00.000, utc);
      
      double mu =  3.986004415e+14; // Earth's gravitational parameter [m^3 s^-2]
      
      double satelliteAltitude = 700000; // Satellite altitude (circular orbit) [m]
      double earthRadius = 6378140; // Earth radius [m]
      
      double a = satelliteAltitude + earthRadius; // Semi-major axis [m]
      double e = 0; // Eccentricity
      double i = Math.toRadians(98.1929); // Inclination [rad]
      double omega = Math.toRadians(0.0); // Perigee argument [rad]
      double raan = Math.toRadians(10.5834); // Right Ascension of Ascending Node [rad]
      double lM = 0; // Mean anomaly [?]
      
      // ORBIT AND CELESTIAL BODIES
      System.out.println("Setting up orbit and celestial bodies..."); // Progress indicator
      Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                                              inertialFrame, initialDate, mu); // Initial orbit for the satellite
      CelestialBody sun  = CelestialBodyFactory.getSun(); // Sun body to calculate vectors
      CelestialBody earthBody  = CelestialBodyFactory.getEarth(); // Earth body to calculate vectors
      
      // GROUND STATION
      System.out.println("Setting up ground station..."); // Progress indicator
      double longitude = Math.toRadians(7.84965); // Freiburg longitude
      double latitude = Math.toRadians(47.6652); // Freiburg latitude
      double altitude = 325.036; // [m]??? 0.325036 km
      GeodeticPoint stationFreiburg = new GeodeticPoint(latitude, longitude, altitude); // Create location point
      // Topocentric frame for the ground station.
      Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
      BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                             Constants.WGS84_EARTH_FLATTENING,
                                             earthFrame); // Shape of the Earth
      TopocentricFrame stationFrame = new TopocentricFrame(earth, stationFreiburg, "Station Freiburg");
      
      // KEPLERIAN PROPAGATOR
      System.out.println("Setting up keplerian propagator..."); // Progress indicator
      KeplerianPropagator kepler = new KeplerianPropagator(initialOrbit);
      
      kepler.setSlaveMode(); // Set mode
      
      // EVENT DETECTOR
      System.out.println("Setting up event detector..."); // Progress indicator
      double maxcheck  = 60.0;
      double threshold =  0.001;
      double elevation = Math.toRadians(10.0); // Minimum elevetion for access
      EventDetector stationVisibility =
        new ElevationDetector(maxcheck, threshold, stationFrame).
        withConstantElevation(elevation).
        withHandler(new VisibilityHandler());
      // Add event detector to the propagator.
      kepler.addEventDetector(stationVisibility);
      
      // FILE WRITERS: file names
      String sunAnglesFileName = "ERNST_i98_1929_a700_SunAngles.csv";
      String earthAnglesFileName = "ERNST_i98_1929_a700_EarthAngles.csv";
      String accessTimesFileName = "ERNST_i98_1929_a700_AccessTimes.csv";
      // FILE WRITERS: PrintWriters
      PrintWriter sunAngles = new PrintWriter(sunAnglesFileName,"UTF-8");
      PrintWriter earthAngles = new PrintWriter(earthAnglesFileName,"UTF-8");
      accessTimes = new PrintWriter(accessTimesFileName,"UTF-8"); // Class variable, already declared
      // File headers
      sunAngles.println("\"Time (UTCG)\",\"Azimuth (deg)\",\"Elevation (deg)\",\"Subsolar (deg)\"");
      earthAngles.println("\"Time (UTCG)\",\"Azimuth (deg)\",\"Elevation (deg)\"");
      accessTimes.println("\"Access\",\"Start Time (UTCG)\",\"Stop Time (UTCG)\",\"Duration (sec)\"");
      
      // PROPAGATION DETAILS
      double duration = 7.0*24.0*60.0*60.0; // [s]
      AbsoluteDate finalDate = initialDate.shiftedBy(duration);
      double stepT = 60.0; // [s]
      
      // PROPAGATION
      System.out.println("Propagating..."); // Progress indicator
      for (AbsoluteDate extrapDate = initialDate; extrapDate.compareTo(finalDate) <= 0; extrapDate = extrapDate.shiftedBy(stepT))  {
        SpacecraftState currentState = kepler.propagate(extrapDate); // Perform propagation
        
        // Get the date, break down into date and time.
        AbsoluteDate absDate = currentState.getDate();
        DateTimeComponents dateTimeComps = absDate.getComponents(0); // Argument 0 means synched with UTC
        DateComponents dateComps = dateTimeComps.getDate();
        TimeComponents timeComps = dateTimeComps.getTime();
        
        // Get month string.
        String monat = dateComps.getMonthEnum().getCapitalizedAbbreviation();
        
        // Get the satellite reference frame.
        LocalOrbitalFrame locFrame =
          new LocalOrbitalFrame(inertialFrame,LOFType.VVLH,
                                currentState.getPVCoordinates().toTaylorProvider(inertialFrame),"Satellite Frame");
        
        // Get the respective coordinates.
        Vector3D sunFromEarth = sun.getPVCoordinates(absDate, inertialFrame).getPosition();
        Vector3D earthFromErnst = earthBody.getPVCoordinates(absDate, locFrame).getPosition();
        Vector3D ernstFromEarth = currentState.getPVCoordinates().getPosition();
        Vector3D sunFromErnst = sun.getPVCoordinates(absDate, locFrame).getPosition();
        
        // Get solar angles.
        double subsol, sunAzim, sunElev;
        subsol= Math.toDegrees(Vector3D.angle(sunFromEarth,ernstFromEarth)); // [deg]
        sunAzim = Math.toDegrees(Vector3D.angle(Vector3D.PLUS_I,sunFromErnst.add(new Vector3D(0.0,0.0,-sunFromErnst.getZ())))); // [deg]
        sunElev = Math.toDegrees(Vector3D.angle(sunFromErnst,new Vector3D(sunFromErnst.getX(),sunFromErnst.getY(),0.0))); // [deg]
        // Fix sign for sunAzim.
        if (sunFromErnst.getY() < 0) {
          sunAzim = 360 - sunAzim;
        }
        // Fix sign for sunElev.
        if (sunFromErnst.getZ() < 0) {
          sunElev = - sunElev;
        }
        
        // Write to solar angles file.
        sunAngles.printf(loc,"%d %s %d %02d:%02d:%06.3f,%07.3f,%07.3f,%07.3f\n",dateComps.getDay(),monat,
                         dateComps.getYear(),timeComps.getHour(),timeComps.getMinute(),timeComps.getSecond(),
                         sunAzim,sunElev,subsol);
        
        // Get Earth angles.
        double earthAzim, earthElev;
        if (earthFromErnst.getX() == 0 & earthFromErnst.getY() == 0) { // Prevents crashing when X and Y are both 0
          earthAzim = 0.0; // [deg]
          earthElev = 90.0; // [deg]
        } else {
          earthAzim = Math.toDegrees(Vector3D.angle(Vector3D.PLUS_I,earthFromErnst.add(new Vector3D(0.0,0.0,-earthFromErnst.getZ())))); // [deg]
          earthElev = Math.toDegrees(Vector3D.angle(earthFromErnst,new Vector3D(earthFromErnst.getX(),earthFromErnst.getY(),0.0))); // [deg]
        }
        // Fix sign for earthAzim.
        if (earthFromErnst.getY() < 0) {
          earthAzim = 360 - earthAzim;
        }
        // Fix sign for sunElev.
        if (earthFromErnst.getZ() < 0) {
          earthElev = - earthElev;
        }
        
        // Write to Earth angles file.
        earthAngles.printf(loc,"%d %s %d %02d:%02d:%06.3f,%07.3f,%07.3f\n",dateComps.getDay(),monat,
                         dateComps.getYear(),timeComps.getHour(),timeComps.getMinute(),timeComps.getSecond(),
                         earthAzim,earthElev);
      }
      
      // Close PrintWriters.
      sunAngles.close();
      earthAngles.close();
      accessTimes.close();
      
      System.out.println("Done."); // Progress indicator
    } catch (Exception e) {
      System.out.println("Error in the main method.\n" + e); // Print the possible error made in the main method.
    }
    
    // Calculate and print elapsed time.
    endTime = System.currentTimeMillis();
    System.out.println("Elapsed time: " + (endTime - startTime)/1000.0 + " seconds.");
    
  } // End of main method.
} // End of class.