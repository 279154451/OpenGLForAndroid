attribute vec4 vPosition; //顶点坐标 变量 float[4]  一个顶点（x,y,z,w）  java传过来的

attribute vec4 vCoord;  //纹理坐标 (x,y,z,w)
//attribute vec2 vCoord;  //纹理坐标
varying vec2 aCoord;//传递给片元着色器的变量，varying修饰，片元着色器程序中需要有个一摸一样的变量定义

uniform mat4 vMatrix;//顶点着色器,用来确定要绘制的几何体形状矩阵
uniform mat4 uMatrix;//正交投影矩阵（4*4）
void main(){
    //内置变量： 把坐标点赋值给gl_position 就Ok了。
    gl_Position = vPosition;
//    gl_Position = uMatrix * vPosition;//正交投影矩阵*顶点坐标（向量）得到正确的归一化设备坐标，能够解决横竖屏宽高比问题
//    aCoord = (vMatrix * vec4(vCoord,1.0,1.0)).xy;
    //通过物体形状矩阵与纹理坐标（x,y,z,w）向量相乘得到物体的像素点坐标，后续片元着色器采样这个像素点的RGBA值并进行绘制渲染
    aCoord = (vMatrix * vCoord).xy;
    gl_PointSize = 10.0;//定义点的绘制粗细
}