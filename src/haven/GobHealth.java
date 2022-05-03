/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.render.*;

public class GobHealth extends GAttrib implements Gob.SetupMod {
    public final float hp;
    public final MixColor fx;
    
    public GobHealth(Gob g, float hp) {
        super(g);
        this.hp = hp;
        if(hp==1) {
            this.fx = null;
        }else if(hp==.75) {
            this.fx = ZeeConfig.MIXCOLOR_YELLOW;
        }else if(hp==.50) {
            this.fx = ZeeConfig.MIXCOLOR_ORANGE;
            if (ZeeManagerMiner.mining)
                ZeeManagerMiner.notifyColumn(g,hp);
        }else {
            this.fx = ZeeConfig.MIXCOLOR_RED;
            if (ZeeManagerMiner.mining)
                ZeeManagerMiner.notifyColumn(g,hp);
        }
    }
    
    public Pipe.Op gobstate() {
	if(hp >= 1)
	    return(null);
	return(fx);
    }

    public double asfloat() {
        return(((double)hp) / 4.0);
    }

    @OCache.DeltaType(OCache.OD_HEALTH)
    public static class $health implements OCache.Delta {
	public void apply(Gob g, Message msg) {
	    int hp = msg.uint8();
	    g.setattr(new GobHealth(g, hp / 4.0f));
	}
    }
}
