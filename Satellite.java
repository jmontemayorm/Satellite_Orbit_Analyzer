/* Back-end of Satellite Orbit Analizer: Satellite Class
 * 
 * Author: Javier Montemayor
 * Created: 2018-06-13
 * Last update: 2018-06-15
 * 
 * To-do:
 * - Set PrintWriters (Dictionary, HashMap) for AccessTimes
 * 
 * To-do in another file (main):
 * - Create the Earth Stations (or geographic zones? <-- add geographic zone detector)
 * - Read file with TLEs, set a list of Satellite objects and create all the propagators, add all the detectors
 * - ...
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

// Satellite Class
public class Satellite implements Runnable {
  // Defaults and Other Finals
  private static final Locale loc = new Locale("EN","US");
  
  // Class Variables
<<<<<<< HEAD
  private AbsoluteDate initialDate, finalDate, accessBegin, accessEnd;
  private double duration, stepT;
  private Propagator propagator;
  private int accessNum = 1; // Reset before every propagation
  private PrintWriter sunAnglesPrinter, earthAnglesPrinter;// log???
  private String sunAnglesName, earthAnglesName, accessTimesName;
  private static DataProvidersManager manager;
  private CelestialBody sunBody, earthBody;
  private Frame inertialFrame;
  private boolean printSun = false, printEarth = false, printAccess = true;
  private Map<String,PrintWriter> accessTimesPrinters = new HashMap<String,PrintWriter>();
=======
  private static AbsoluteDate initialDate, finalDate, accessBegin, accessEnd;
  private static double duration, stepT;
  private static Propagator propagator;
  private static int accessNum = 1; // Reset before every propagation
  private static PrintWriter sunAnglesPrinter, earthAnglesPrinter;// log???
  private static String sunAnglesName, earthAnglesName, accessTimesName;
  private static DataProvidersManager manager;
  private static CelestialBody sunBody, earthBody;
  private static Frame inertialFrame;
  private static boolean printSun = false, printEarth = false, printAccess = true;
  private static Map<String,PrintWriter> accessTimesPrinters = new HashMap<String,PrintWriter>();
>>>>>>> 9e1fd8fd2dcc786b40fa85bdec01ea1271e2bee9
  
  // Satellite Constructor
  public Satellite() {
    try {
      // Try default configuration
      File orekitData = new File("Libraries/orekit-data");
      manager = DataProvidersManager.getInstance();
      manager.addProvider(new DirectoryCrawler(orekitData));
      
      // Set celestial bodies
      sunBody = CelestialBodyFactory.getSun();
      earthBody = CelestialBodyFactory.getEarth();
      
      // Set inertial frame
      inertialFrame = FramesFactory.getEME2000();
      
      ////////////////////////////////////// TEMPORARY CODE FOR TESTING ///////////////////////////////////////////////
      initialDate = new AbsoluteDate(2021, 01, 01, 00, 00, 00.000, TimeScalesFactory.getUTC());
<<<<<<< HEAD
      duration = 30.0*24.0*60.0*60.0; // [s]
=======
      duration = 24.0*60.0*60.0; // [s]
>>>>>>> 9e1fd8fd2dcc786b40fa85bdec01ea1271e2bee9
      finalDate = initialDate.shiftedBy(duration);
      stepT = 60.0;
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    } catch (Exception e) {
      System.out.println("Error on Satellite constructor: " + e);
    }
  }
  
  // Turn on Sun Angles printer
  public void turnOnSunAnglesPrinter() {
    printSun = true;
  }
  
  // Turn off Sun Angles printer
  public void turnOffSunAnglesPrinter() {
    printSun = false;
  }
  
  // Turn on Earth Angles printer
  public void turnOnEarthAnglesPrinter() {
    printEarth = true;
  }
  
  // Turn off Earth Angles printer
  public void turnOffEarthAnglesPrinter() {
    printEarth = false;
  }
  
  // Sets a TLE propagator
  public boolean setTLEPropagator(String line1, String line2) {
    try {
      // Set propagator (if there was a previous one, it will be replaced)
      TLE tle = new TLE(line1,line2);
<<<<<<< HEAD
      propagator = TLEPropagator.selectExtrapolator(tle);
=======
      int satMass = 1000; /////////// CHECK IF THIS COULD BE CHANGED
      propagator = new SGP4(tle,Propagator.DEFAULT_LAW,satMass);
>>>>>>> 9e1fd8fd2dcc786b40fa85bdec01ea1271e2bee9
      
      propagator.setSlaveMode();
      
      // Success, return true
      return true;
    } catch (Exception e) {
      System.out.println("Error setting TLE: " + e);
    }
    
    // An error occurred, return false
    return false;
  }
  
  // Sets an elevation detector for the given station
  public boolean setElevationDetector(TopocentricFrame station, double maxCheck, double threshold, double elevationDeg) {
    // If no propagator has been provided, return false
    if (propagator == null)
      return false;
    
    try {
      ElevationDetector elevDetect =
        new ElevationDetector(maxCheck, threshold, station
                             ).withConstantElevation(Math.toRadians(elevationDeg)
                                                    ).withHandler(new VisibilityHandler());
      propagator.addEventDetector(elevDetect);
      // Success, return true
      return true;
    } catch (Exception e) {
      System.out.println("Error setting elevation detector: " + e);
    }
    
    // An error occurred, return false
    return false;
  }
  
  // Starts propagation, returns false if something has not been setup // CHANGE NAME TO RUN()?
  public void run() {
    // Check if everything is set to perform propagation /////////////////////////////////////////////////////////////////////////// CORRECT FOR WHEN PRINTER INACTIVE DOESNT MATTER
    if (propagator == null || initialDate == null || finalDate == null || stepT == 0) {
      // Something is missing (log error?), return false
      System.out.println("The satellite has not been fully set.");
      //return false;
    } else {
      // All should be set to perform the propagation
      try {
        // Reset access counter, set accessBegin as initialDate in case propagation starts during an access
        accessNum = 1; // <-- Needs one variable per detector, dictionary
        accessBegin = initialDate;
        
        // Set PrintWriters
        if (printSun)
          sunAnglesPrinter = new PrintWriter(sunAnglesName,"UTF-8");
        if (printEarth)
          earthAnglesPrinter = new PrintWriter(earthAnglesName,"UTF-8");
        
        for (AbsoluteDate extrapDate = initialDate; extrapDate.compareTo(finalDate) <= 0; extrapDate = extrapDate.shiftedBy(stepT)) {
<<<<<<< HEAD
          try {
            // Get current state
            SpacecraftState currentState = propagator.propagate(extrapDate);
            
//            // Get the absolute date, break down into date and time
//            AbsoluteDate absDate = currentState.getDate();
//            DateTimeComponents dateTimeComps = absDate.getComponents(0); // Synched with UTC
//            DateComponents dateComps = dateTimeComps.getDate();
//            TimeComponents timeComps = dateTimeComps.getTime();
//            
//            // Get month string
//            String monat = dateComps.getMonthEnum().getCapitalizedAbbreviation();
//            
//            // Get the satellite reference frame
//            LocalOrbitalFrame locFrame =
//              new LocalOrbitalFrame(inertialFrame,LOFType.VVLH,
//                                    currentState.getPVCoordinates().toTaylorProvider(inertialFrame),"Satellite Frame");
//            
//            // If active, print Solar Angles
//            if (printSun) {
//              // Get the respective coordinates
//              Vector3D sunFromEarth = sunBody.getPVCoordinates(absDate, inertialFrame).getPosition();
//              Vector3D satFromEarth = currentState.getPVCoordinates().getPosition();
//              Vector3D sunFromSat = sunBody.getPVCoordinates(absDate, locFrame).getPosition();
//              
//              // Get solar angles
//              double subsol, sunAzim, sunElev;
//              subsol= Math.toDegrees(Vector3D.angle(sunFromEarth,satFromEarth)); // [deg]
//              sunAzim = Math.toDegrees(Vector3D.angle(Vector3D.PLUS_I,sunFromSat.add(new Vector3D(0.0,0.0,-sunFromSat.getZ())))); // [deg]
//              sunElev = Math.toDegrees(Vector3D.angle(sunFromSat,new Vector3D(sunFromSat.getX(),sunFromSat.getY(),0.0))); // [deg]
//              // Fix sign for sunAzim
//              if (sunFromSat.getY() < 0) {
//                sunAzim = 360 - sunAzim;
//              }
//              // Fix sign for sunElev
//              if (sunFromSat.getZ() < 0) {
//                sunElev = - sunElev;
//              }
//              
//              // Write to solar angles file
//              sunAnglesPrinter.printf(loc,"%d %s %d %02d:%02d:%06.3f,%07.3f,%07.3f,%07.3f\n",dateComps.getDay(),monat,
//                                      dateComps.getYear(),timeComps.getHour(),timeComps.getMinute(),timeComps.getSecond(),
//                                      sunAzim,sunElev,subsol);
//            }
//            
//            // If active, print Earth Angles
//            if (printEarth) {
//              // Get the respective coordinates
//              Vector3D earthFromSat = earthBody.getPVCoordinates(absDate, locFrame).getPosition();
//              
//              // Get Earth angles
//              double earthAzim, earthElev;
//              if (earthFromSat.getX() == 0 & earthFromSat.getY() == 0) { // Prevents crashing when X and Y are both 0
//                earthAzim = 0.0; // [deg]
//                if (earthFromSat.getZ() > 0)
//                  earthElev = 90.0; // [deg]
//                else
//                  earthElev = -90.0; // [deg]
//              } else {
//                earthAzim = Math.toDegrees(Vector3D.angle(Vector3D.PLUS_I,earthFromSat.add(new Vector3D(0.0,0.0,-earthFromSat.getZ())))); // [deg]
//                earthElev = Math.toDegrees(Vector3D.angle(earthFromSat,new Vector3D(earthFromSat.getX(),earthFromSat.getY(),0.0))); // [deg]
//              }
//              // Fix sign for earthAzim
//              if (earthFromSat.getY() < 0) {
//                earthAzim = 360 - earthAzim;
//              }
//              // Fix sign for sunElev
//              if (earthFromSat.getZ() < 0) {
//                earthElev = - earthElev;
//              }
//              
//              // Write to Earth angles file
//              earthAnglesPrinter.printf(loc,"%d %s %d %02d:%02d:%06.3f,%07.3f,%07.3f\n",dateComps.getDay(),monat,
//                                        dateComps.getYear(),timeComps.getHour(),timeComps.getMinute(),timeComps.getSecond(),
//                                        earthAzim,earthElev);
//            }
          } catch (Exception e) {
            System.out.println("Error inside the loop: " + e);
=======
          // Get current state
          SpacecraftState currentState = propagator.propagate(extrapDate);
          
          // Get the absolute date, break down into date and time
          AbsoluteDate absDate = currentState.getDate();
          DateTimeComponents dateTimeComps = absDate.getComponents(0); // Synched with UTC
          DateComponents dateComps = dateTimeComps.getDate();
          TimeComponents timeComps = dateTimeComps.getTime();
          
          // Get month string
          String monat = dateComps.getMonthEnum().getCapitalizedAbbreviation();
          
          // Get the satellite reference frame
          LocalOrbitalFrame locFrame =
            new LocalOrbitalFrame(inertialFrame,LOFType.VVLH,
                                  currentState.getPVCoordinates().toTaylorProvider(inertialFrame),"Satellite Frame");
          
          // If active, print Solar Angles
          if (printSun) {
            // Get the respective coordinates
            Vector3D sunFromEarth = sunBody.getPVCoordinates(absDate, inertialFrame).getPosition();
            Vector3D satFromEarth = currentState.getPVCoordinates().getPosition();
            Vector3D sunFromSat = sunBody.getPVCoordinates(absDate, locFrame).getPosition();
            
            // Get solar angles
            double subsol, sunAzim, sunElev;
            subsol= Math.toDegrees(Vector3D.angle(sunFromEarth,satFromEarth)); // [deg]
            sunAzim = Math.toDegrees(Vector3D.angle(Vector3D.PLUS_I,sunFromSat.add(new Vector3D(0.0,0.0,-sunFromSat.getZ())))); // [deg]
            sunElev = Math.toDegrees(Vector3D.angle(sunFromSat,new Vector3D(sunFromSat.getX(),sunFromSat.getY(),0.0))); // [deg]
            // Fix sign for sunAzim
            if (sunFromSat.getY() < 0) {
              sunAzim = 360 - sunAzim;
            }
            // Fix sign for sunElev
            if (sunFromSat.getZ() < 0) {
              sunElev = - sunElev;
            }
            
            // Write to solar angles file
            sunAnglesPrinter.printf(loc,"%d %s %d %02d:%02d:%06.3f,%07.3f,%07.3f,%07.3f\n",dateComps.getDay(),monat,
                                    dateComps.getYear(),timeComps.getHour(),timeComps.getMinute(),timeComps.getSecond(),
                                    sunAzim,sunElev,subsol);
          }
          
          // If active, print Earth Angles
          if (printEarth) {
            // Get the respective coordinates
            Vector3D earthFromSat = earthBody.getPVCoordinates(absDate, locFrame).getPosition();
            
            // Get Earth angles
            double earthAzim, earthElev;
            if (earthFromSat.getX() == 0 & earthFromSat.getY() == 0) { // Prevents crashing when X and Y are both 0
              earthAzim = 0.0; // [deg]
              if (earthFromSat.getZ() > 0)
                earthElev = 90.0; // [deg]
              else
                earthElev = -90.0; // [deg]
            } else {
              earthAzim = Math.toDegrees(Vector3D.angle(Vector3D.PLUS_I,earthFromSat.add(new Vector3D(0.0,0.0,-earthFromSat.getZ())))); // [deg]
              earthElev = Math.toDegrees(Vector3D.angle(earthFromSat,new Vector3D(earthFromSat.getX(),earthFromSat.getY(),0.0))); // [deg]
            }
            // Fix sign for earthAzim
            if (earthFromSat.getY() < 0) {
              earthAzim = 360 - earthAzim;
            }
            // Fix sign for sunElev
            if (earthFromSat.getZ() < 0) {
              earthElev = - earthElev;
            }
            
            // Write to Earth angles file
            earthAnglesPrinter.printf(loc,"%d %s %d %02d:%02d:%06.3f,%07.3f,%07.3f\n",dateComps.getDay(),monat,
                                      dateComps.getYear(),timeComps.getHour(),timeComps.getMinute(),timeComps.getSecond(),
                                      earthAzim,earthElev);
>>>>>>> 9e1fd8fd2dcc786b40fa85bdec01ea1271e2bee9
          }
        }
        
        // Close PrintWriters
        if (printSun)
          sunAnglesPrinter.close();
        if (printEarth)
          earthAnglesPrinter.close();
        
      }catch (Exception e) {
        System.out.println("Error while running: " + e);
      }
      System.out.println("Finished propagation.");
      //return true;
    }
  }
  
  // Visibility Hanlder for Elevation Detectors
<<<<<<< HEAD
  private class VisibilityHandler implements EventHandler<ElevationDetector> {
    public Action eventOccurred(final SpacecraftState s, final ElevationDetector detector,
                                final boolean increasing) {
      if (s.getDate().compareTo(initialDate) <= 0)
        return Action.STOP;
=======
  private static class VisibilityHandler implements EventHandler<ElevationDetector> {
    public Action eventOccurred(final SpacecraftState s, final ElevationDetector detector,
                                final boolean increasing) {
      if (s.getDate().compareTo(initialDate) <= 0)
          return Action.STOP;
>>>>>>> 9e1fd8fd2dcc786b40fa85bdec01ea1271e2bee9
      if (increasing) {
        accessBegin = s.getDate();
        
        return Action.CONTINUE;
      } else {
        accessEnd = s.getDate();
        
<<<<<<< HEAD
        try{
          double visDuration = accessEnd.durationFrom(accessBegin);
          DateTimeComponents compsBegin = accessBegin.getComponents(0); // Synched with UTC
          DateTimeComponents compsEnd = accessEnd.getComponents(0); // Synched with UTC
          DateComponents dateBegin = compsBegin.getDate();
          DateComponents dateEnd = compsEnd.getDate();
          TimeComponents timeBegin = compsBegin.getTime();
          TimeComponents timeEnd = compsEnd.getTime();
          
          //accessTimesPrinters.get(detector.getTopocentricFrame().getName()).
          System.out.printf(loc,"%d,%d %s %d %02d:%02d:%06.3f,%d %s %d %02d:%02d:%06.3f,%07.3f\n",accessNum++,
                            dateBegin.getDay(),dateBegin.getMonthEnum().getCapitalizedAbbreviation(),dateBegin.getYear(),
                            timeBegin.getHour(),timeBegin.getMinute(),timeBegin.getSecond(),dateEnd.getDay(),
                            dateEnd.getMonthEnum().getCapitalizedAbbreviation(),dateEnd.getYear(),timeEnd.getHour(),
                            timeEnd.getMinute(),timeEnd.getSecond(),visDuration);
        } catch (Exception ex) {
          System.out.println("Error calculating or writing to Access Times file: " + ex);
        }
=======
//        try{
//          double visDuration = accessEnd.durationFrom(accessBegin);
//          DateTimeComponents compsBegin = accessBegin.getComponents(0); // Synched with UTC
//          DateTimeComponents compsEnd = accessEnd.getComponents(0); // Synched with UTC
//          DateComponents dateBegin = compsBegin.getDate();
//          DateComponents dateEnd = compsEnd.getDate();
//          TimeComponents timeBegin = compsBegin.getTime();
//          TimeComponents timeEnd = compsEnd.getTime();
//          
//          //accessTimesPrinters.get(detector.getTopocentricFrame().getName()).
//          System.out.printf(loc,"%d,%d %s %d %02d:%02d:%06.3f,%d %s %d %02d:%02d:%06.3f,%07.3f\n",accessNum++,
//                            dateBegin.getDay(),dateBegin.getMonthEnum().getCapitalizedAbbreviation(),dateBegin.getYear(),
//                            timeBegin.getHour(),timeBegin.getMinute(),timeBegin.getSecond(),dateEnd.getDay(),
//                            dateEnd.getMonthEnum().getCapitalizedAbbreviation(),dateEnd.getYear(),timeEnd.getHour(),
//                            timeEnd.getMinute(),timeEnd.getSecond(),visDuration);
//        } catch (Exception ex) {
//          System.out.println("Error calculating or writing to Access Times file: " + ex);
//        }
>>>>>>> 9e1fd8fd2dcc786b40fa85bdec01ea1271e2bee9
        
        return Action.STOP;
      }
    }
  }
}