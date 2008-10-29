/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 */

package org.glassfish.embed;


import com.sun.enterprise.universal.io.SmartFile;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;

/**
 * Launches a mock-up HK2 environment that doesn't provide
 * any classloader isolation. Instead, the whole thing is loaded
 * from the single classloader.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main_new {
    public static void main(String[] args) {
        try {

            pr("");
            pr("A Very Simple Sample of Using Embedded GlassFish");
            pr("");
            File war = getWar();
            Server.setLogLevel(INFO);
            Server glassfish = new Server(9999);
            Application app = glassfish.deploy(war);
            pr(war.toString() + " deployed. GlassFish is listening at port 9999 for HTTP traffic.");

            char key = Console.getKey("Hit Enter to stop the server and exit.  Enter 'X' to *not* call System.exit()");
            app.undeploy();
            pr("Application undeployed");
            glassfish.stop();
            pr("Server stopped");
            
            if(key != 'X')
                System.exit(0);
            
        }
        catch (EmbeddedException ex) {
            Logger.getLogger(Main_new.class.getName()).log(Level.SEVERE, null, ex);
        }        
        catch (Exception ex) {
            error(ex.toString() + ex);
        }
    }


    private static File getWar() {
        String warFileName = null;

        do {
            warFileName = Console.readLine("Enter a war filename for deployment [X to exit]");
        } while(warFileName == null || warFileName.length() == 0);
        
        if(warFileName.equals("X")) {
            pr("exiting per your request");
            System.exit(0);
        }
        
        File war = SmartFile.sanitize(new File(warFileName));
        
        if(!war.exists())
            error("File does not exist: " + war);
        if(war.isDirectory())
            error("File is a directory: " + war);
        
        return war;
    }

    public static void error(String err) {
        pr("ERROR: " + err);
        System.exit(-1);
    }
    
    private static void pr(String msg) {
        System.out.println("***** " + msg);
    }
    
}
