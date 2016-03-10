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

package haven.resutil;

import haven.*;
import haven.glsl.*;
import java.util.*;
import java.nio.*;
import javax.media.opengl.*;
import static haven.glsl.Cons.*;
import static haven.glsl.Type.*;

public class OverTex extends GLState {
    public static final Slot<OverTex> slot = new Slot<OverTex>(Slot.Type.DRAW, OverTex.class);
    public static final Attribute otexc = new Attribute(VEC2);
    private static final Uniform ctex = new Uniform(SAMPLER2D);
    public final TexGL tex;
    private TexUnit sampler;

    public OverTex(TexGL tex) {
	this.tex = tex;
    }

    private static final ShaderMacro[] shaders = {
	new ShaderMacro() {
	    final AutoVarying otexcv = new AutoVarying(VEC2, "otexc") {
		    protected Expression root(VertexContext vctx) {
			return(otexc.ref());
		    }
		};
	    public void modify(final ProgramContext prog) {
		final ValBlock.Value color = prog.fctx.uniform.new Value(VEC4) {
			public Expression root() {
			    return(texture2D(ctex.ref(), otexcv.ref()));
			}
		    };
		color.force();
		prog.fctx.fragcol.mod(new Macro1<Expression>() {
			public Expression expand(Expression in) {
			    return(MiscLib.olblend.call(in, color.ref()));
			}
		    }, 10);
	    }
	}
    };

    public ShaderMacro[] shaders() {return(shaders);}

    public void reapply(GOut g) {
	g.gl.glUniform1i(g.st.prog.uniform(ctex), sampler.id);
    }

    public void apply(GOut g) {
	sampler = TexGL.lbind(g, tex);
	reapply(g);
    }

    public void unapply(GOut g) {
	BGL gl = g.gl;
	sampler.act(g);
	gl.glBindTexture(GL.GL_TEXTURE_2D, null);
	sampler.free(); sampler = null;
    }

    public void prep(Buffer buf) {
	buf.put(slot, this);
    }

    @Material.ResName("otex")
    public static class $ctex implements Material.ResCons2 {
	public Material.Res.Resolver cons(final Resource res, Object... args) {
	    final Indir<Resource> tres;
	    final int tid;
	    int a = 0;
	    if(args[a] instanceof String) {
		tres = res.pool.load((String)args[a], (Integer)args[a + 1]);
		tid = (Integer)args[a + 2];
		a += 3;
	    } else {
		tres = res.indir();
		tid = (Integer)args[a];
		a += 1;
	    }
	    return(new Material.Res.Resolver() {
		    public void resolve(Collection<GLState> buf) {
			TexR rt = tres.get().layer(TexR.class, tid);
			if(rt == null)
			    throw(new RuntimeException(String.format("Specified texture %d for %s not found in %s", tid, res, tres)));
			/* XXX: It is somewhat doubtful that this cast is really quite reasonable. */
			buf.add(new OverTex((TexGL)rt.tex()));
		    }
		});
	}
    }

    @VertexBuf.ResName("otex")
    public static class OTexC extends VertexBuf.Vec2Array {
	public OTexC(FloatBuffer data) {super(data, otexc);}
	public OTexC(Resource res, Message buf, int nv) {this(VertexBuf.loadbuf(Utils.wfbuf(nv * 2), buf));}
    }
}
