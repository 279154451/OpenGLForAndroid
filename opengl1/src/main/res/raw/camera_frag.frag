#extension GL_OES_EGL_image_external : require
//摄像头数据比较特殊的一个地方
precision mediump float; // 数据精度
varying vec2 aCoord;

uniform samplerExternalOES  vTexture;  // samplerExternalOES: 图片， 采样器
//片元着色器，用来绘制上色
void main(){
    //  texture2D: vTexture采样器，采样aCoord这个像素点的RGBA值
        gl_FragColor =texture2D(vTexture,aCoord);
//    vec4 rgba = texture2D(vTexture,aCoord);  //rgba
//    float r = 0.33*rgba.a+0.59*rgba.g+0.11*rgba.b;//这里是利用305911公式来达到灰度化效果
//    gl_FragColor = vec4(r,r,r,rgba.a);

}