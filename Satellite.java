/* Back-end of Satellite Orbit Analizer: Satellite Class
 * 
 * Author: Javier Montemayor
 * Created: 2018-06-13
 * Last update: 2018-06-25
 * 
 * To-do:
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
  private AbsoluteDate initialDate, finalDate;
  private double duration, stepT;
  private Propagator propagator;
  private static DataProvidersManager manager;
  private CelestialBody sunBody, earthBody;
  private Frame inertialFrame;
  private String satName;
  
  // Printing Related Class Variables
  private boolean printSun = true, printEarth = true, printAccess = true;
  private String sunAnglesName, earthAnglesName;
  private PrintWriter sunAnglesPrinter, earthAnglesPrinter;// log???
  private Map<String,PrintWriter> accessTimesPrinters = new HashMap<String,PrintWriter>();
  private Map<String,Integer> accessNum = new HashMap<String,Integer>();
  private Map<String,AbsoluteDate> accessBegin = new HashMap<String,AbsoluteDate>();
  private Map<String,AbsoluteDate> accessEnd = new HashMap<String,AbsoluteDate>(); // Probably not required
  
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
      
    } catch (Exception e) {
      System.out.println("Error on Satellite constructor: " + e);
    }
  }
  
  ////////////////////////////////////////////// SET PROPAGATOR METHODS ///////////////////////////////////////////////
  // Sets a TLE propagator
  public boolean setTLEPropagator(String line1, String line2, String name) {
    try {
      // Set propagator (if there was a previous one, it will be replaced)
      TLE tle = new TLE(line1,line2);
      propagator = TLEPropagator.selectExtrapolator(tle);
      
      propagator.setSlaveMode();
      
      satName = name;
      
      
      // Success, return true
      return true;
    } catch (Exception e) {
      System.out.println("Error setting TLE: " + e);
    }
    
    // An error occurred, return false
    return false;
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  //////////////////////////////////////////////// SET PRINTER METHODS ////////////////////////////////////////////////
  // Turn on/off Sun Angles printer (default is true)
  public void printSunAngles(boolean state) {
    printSun = state;
  }
  
  // Turn on/off Earth Angles printer (default is true)
  public void printEarthAngles(boolean state) {
    printEarth = state;
  }
  
  // Turn on/off Access Times printers (default is true)
  public void printAccessTimes(boolean state) {
    printAccess = state;
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////// INDIVIDUAL SET METHODS ///////////////////////////////////////////////
  // Sets an initial date
  public void setInitialDate(AbsoluteDate date) {
    initialDate = date;
  }
  
  // Sets a duration
  public boolean setDuration(double s) {
    if (initialDate == null || s <= 0.0)
      return false;
    
    duration = s;
    finalDate = initialDate.shiftedBy(duration);
    return true;
  }
  
  // Sets a final date
  public boolean setFinalDate(AbsoluteDate date) {
    if (initialDate == null)
      return false;
    
    finalDate = date;
    duration = finalDate.durationFrom(initialDate);
    
    if (duration <= 0.0) {
      finalDate = null;
      return false;
    }
    
    return true;
  }
  
  // Sets a step size
  public boolean setStep(double s) {
    if (s <= 0.0)
      return false;
    
    stepT = s;
    return true;
  }
  
  // Sets the sun angles filename
  public boolean setSunPath(String p) {
    if (satName == null) {
      System.out.println("Please set the satellite name first.");
      return false;
    }
    
    sunAnglesName = p + "/" + satName + "_sunAngles.csv";
    return true;
  }
  
  // Sets the earth angles filename
  public boolean setEarthPath(String p) {
    if (satName == null) {
      System.out.println("Please set the satellite name first.");
      return false;
    }
    
    earthAnglesName = p + "/" + satName + "_earthAngles.csv";
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  ///////////////////////////////////////////////// "SET ALL" METHODS /////////////////////////////////////////////////
  // Sets all (initial date, final date by duration, step size)
  public boolean setAll(AbsoluteDate iDate, double dur, double step) {
    setInitialDate(iDate);
    
    return setDuration(dur) && setStep(step);
  }
  
  // Sets all (initial date, final date, step size)
  public boolean setAll(AbsoluteDate iDate, AbsoluteDate fDate, double step) {
    setInitialDate(iDate);
    
    return setFinalDate(fDate) && setStep(step);
  }
  
  // Sets all (initial date, final date by duration, step size, TLE prop)
  public boolean setAll(AbsoluteDate iDate, double dur, double step, String line1, String line2, String sName) {
    setInitialDate(iDate);
    
    return setDuration(dur) && setStep(step) && setTLEPropagator(line1, line2, sName);
  }
  
  // Sets all (initial date, final date, step size, TLE prop)
  public boolean setAll(AbsoluteDate iDate, AbsoluteDate fDate, double step, String line1, String line2, String sName) {
    setInitialDate(iDate);
    
    return setFinalDate(fDate) && setStep(step) && setTLEPropagator(line1, line2, sName);
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /////////////////////////////////////////////// SET DETECTOR METHODS ////////////////////////////////////////////////
  // Sets an elevation detector for the given station
  public boolean setElevationDetector(TopocentricFrame station, double maxCheck, double threshold, double elevationDeg,
                                      String accessPath) {
    // If no propagator has been provided, return false
    if (propagator == null)
      return false;
    
    try {
      ElevationDetector elevDetect =
        new ElevationDetector(maxCheck, threshold, station
                             ).withConstantElevation(Math.toRadians(elevationDeg)
                                                    ).withHandler(new VisibilityHandler());
      propagator.addEventDetector(elevDetect);
      
      // Create the station folder in case it does not exist //// THIS COULD BE DONE BEFORE?
      String stationFolder = accessPath + "/" + station.getName();
      File stationDir = new File(stationFolder);
      if (!stationDir.exists())
        stationDir.mkdirs();
      
      String fName = stationFolder + "/" + satName + ".csv";
      
      if (printAccess) {
        accessTimesPrinters.put(station.getName(), new PrintWriter(fName,"UTF-8"));
        accessTimesPrinters.get(station.getName()).
          println("\"Access\",\"Start Time (UTCG)\",\"Stop Time (UTCG)\",\"Duration (sec)\"");
      }
      
      // Success, return true
      return true;
    } catch (Exception e) {
      System.out.println("Error setting elevation detector: " + e);
    }
    
    // An error occurred, return false
    return false;
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////// RUN //////////////////////////////////////////////////////////
  // Starts propagation, returns false if something has not been setup
  public void run() {
    if (propagator == null || initialDate == null || finalDate == null || stepT == 0) {
      // Something is missing (log error?), return false
      System.out.println("The satellite has not been fully set.");
    } else {
      // All should be set to perform the propagation
      try {
        // Reset or initializes access counter, set accessBegin as initialDate in case propagation starts during an access
        for (String key : accessTimesPrinters.keySet()) {
          accessNum.put(key,1);
          accessBegin.put(key,initialDate);
        }
        
        // Set PrintWriters
        if (printSun) {
          sunAnglesPrinter = new PrintWriter(sunAnglesName,"UTF-8");
          sunAnglesPrinter.println("\"Time (UTCG)\",\"Azimuth (deg)\",\"Elevation (deg)\",\"Subsolar (deg)\"");
        }
        if (printEarth) {
          earthAnglesPrinter = new PrintWriter(earthAnglesName,"UTF-8");
          earthAnglesPrinter.println("\"Time (UTCG)\",\"Azimuth (deg)\",\"Elevation (deg)\"");
        }
        
        for (AbsoluteDate extrapDate = initialDate; extrapDate.compareTo(finalDate) <= 0; extrapDate = extrapDate.shiftedBy(stepT)) {
          // Get current state
          SpacecraftState currentState = propagator.propagate(extrapDate);
          
          // Get the absolute date, break down into date and time
          AbsoluteDate absDate = currentState.getDate();
          DateTimeComponents dateTimeComps = absDate.getComponents(0); // Synched with UTC
          DateComponents dateComps = dateTimeComps.getDate();
          TimeComponents timeComps = dateTimeComps.getTime();
          
          // If its before the initial date, skip
//          if (absDate.compareTo(initialDate) <= 0)
//            continue;
          
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
          }
        }
        
        // Close PrintWriters
        if (printSun)
          sunAnglesPrinter.close();
        if (printEarth)
          earthAnglesPrinter.close();
        
        if (printAccess) {
          for (PrintWriter pWriter : accessTimesPrinters.values()) {
            pWriter.close();
          }
        }
        
      }catch (Exception e) {
        System.out.println("Error while running: " + e);
      }
      System.out.println("Finished propagation.");
    }
  }
  
  // Visibility Hanlder for Elevation Detectors
  private class VisibilityHandler implements EventHandler<ElevationDetector> {
    public Action eventOccurred(final SpacecraftState s, final ElevationDetector detector,
                                final boolean increasing) {
      if (s.getDate().compareTo(initialDate) <= 0)
        return Action.STOP;
      
      if (increasing) {
        accessBegin.put(detector.getTopocentricFrame().getName(),s.getDate());
        
        return Action.CONTINUE;
      } else {
        //accessEnd.put(detector.getTopocentricFrame().getName(),s.getDate());
        
        AbsoluteDate aEnd = s.getDate();
        
        if (printAccess) {
          try{
            double visDuration = aEnd.durationFrom(accessBegin.get(detector.getTopocentricFrame().getName()));
            DateTimeComponents compsBegin =
              accessBegin.get(detector.getTopocentricFrame().getName()).getComponents(0); // Synched with UTC
            DateTimeComponents compsEnd = aEnd.getComponents(0); // Synched with UTC
            DateComponents dateBegin = compsBegin.getDate();
            DateComponents dateEnd = compsEnd.getDate();
            TimeComponents timeBegin = compsBegin.getTime();
            TimeComponents timeEnd = compsEnd.getTime();
            
            int aNum = accessNum.get(detector.getTopocentricFrame().getName());
            accessTimesPrinters.get(detector.getTopocentricFrame().getName()).
              printf(loc,"%d,%d %s %d %02d:%02d:%06.3f,%d %s %d %02d:%02d:%06.3f,%07.3f\n",aNum++,
                     dateBegin.getDay(),dateBegin.getMonthEnum().getCapitalizedAbbreviation(),dateBegin.getYear(),
                     timeBegin.getHour(),timeBegin.getMinute(),timeBegin.getSecond(),dateEnd.getDay(),
                     dateEnd.getMonthEnum().getCapitalizedAbbreviation(),dateEnd.getYear(),timeEnd.getHour(),
                     timeEnd.getMinute(),timeEnd.getSecond(),visDuration);
            accessNum.put(detector.getTopocentricFrame().getName(),aNum);
          } catch (Exception ex) {
            System.out.println("Error calculating or writing to Access Times file: " + ex);
          }
        }
        return Action.STOP;
      }
    }
  }
}