package com.qi.tai.opengl.base.gles.shader;

import android.opengl.GLES20;

import com.qi.tai.opengl.base.gles.IShaderProvider;
import com.qi.tai.opengl.base.gles.utils.OpenGLUtils;

/**
 * 创建时间：2022/4/17
 * 创建人：singleCode
 * 功能描述：
 **/
public class ShaderProviderYUV extends IShaderProvider {
    //着色器程序定义，GLSL语言
    private final static String VertexShader =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec2 vCoord;" +
            "varying vec2 aCoord;" +
            "uniform mat4 vMatrix;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  aCoord = vCoord;" +
            "}";
    private final static String FragmentShader =
            "precision mediump float;" +
                    "uniform sampler2D samplerY;" +
                    "uniform sampler2D samplerU;" +
                    "uniform sampler2D samplerV;" +
                    "uniform sampler2D samplerUV;" +
                    "uniform int yuvType;" +
                    "varying vec2 aCoord;" +
                    "void main() {" +
                    "  vec4 c = vec4((texture2D(samplerY, aCoord).r - 16./255.) * 1.164);" +
                    "  vec4 U; vec4 V;" +
                    "  if (yuvType == 0){" +
                    "    U = vec4(texture2D(samplerU, aCoord).r - 128./255.);" +
                    "    V = vec4(texture2D(samplerV, aCoord).r - 128./255.);" +
                    "  } else if (yuvType == 1){" +
                    "    U = vec4(texture2D(samplerUV, aCoord).r - 128./255.);" +
                    "    V = vec4(texture2D(samplerUV, aCoord).a - 128./255.);" +
                    "  } else {" +
                    "    U = vec4(texture2D(samplerUV, aCoord).a - 128./255.);" +
                    "    V = vec4(texture2D(samplerUV, aCoord).r - 128./255.);" +
                    "  } " +
                    "  c += V * vec4(1.596, -0.813, 0, 0);" +
                    "  c += U * vec4(0, -0.392, 2.017, 0);" +
                    "  c.a = 1.0;" +
                    "  gl_FragColor = c;" +
                    "}";

    public ShaderProviderYUV() {
        super(OpenGLUtils.VERTEX, OpenGLUtils.TEXTURE_Android);
    }

    @Override
    public int getTextureIdTarget() {
        return GLES20.GL_TEXTURE_2D;
    }
    @Override
    public String getVertexShader() {
        return VertexShader;
    }

    @Override
    public String getFragmentShader() {
        return FragmentShader;
    }
}
