attribute vec4 vPosition; //变量 float[4]  一个顶点  java传过来的

attribute vec4 vCoord;  //纹理坐标
//attribute vec2 vCoord;  //纹理坐标
varying vec2 aCoord;//传递给片元着色器的变量，varying修饰，片元着色器程序中需要有个一摸一样的变量定义

uniform mat4 vMatrix;
//顶点着色器,用来确定要绘制的几何体形状
void main(){
    //内置变量： 把坐标点赋值给gl_position 就Ok了。
    gl_Position = vPosition;
//    aCoord = (vMatrix * vec4(vCoord,1.0,1.0)).xy;
    aCoord = (vMatrix * vCoord).xy;
}