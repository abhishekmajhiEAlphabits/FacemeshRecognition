package com.example.facemeshrecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.ImageWriter;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import java.nio.ByteBuffer;

public class Util {

//    public static Bitmap bitmapFromRgba(int width, int height, byte[] bytes) {
//        int[] pixels = new int[bytes.length / 4];
//        int j = 0;
//
//        // It turns out Bitmap.Config.ARGB_8888 is in reality RGBA_8888!
//        // Source: https://stackoverflow.com/a/47982505/1160360
//        // Now, according to my own experiments, it seems it is ABGR... this sucks.
//        // So we have to change the order of the components
//
//        for (int i = 0; i < pixels.length; i++) {
//            byte R = bytes[j++];
//            byte G = bytes[j++];
//            byte B = bytes[j++];
//            byte A = bytes[j++];
//
//            int pixel = (A << 24) | (B << 16) | (G << 8) | R;
//            pixels[i] = pixel;
//        }
//
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixels));
//        return bitmap;
//    }

    public static Bitmap bitmapFromRgba(int width, int height, byte[] bytes) {
        int[] pixels = new int[bytes.length / 4];
        int j = 0;

        for (int i = 0; i < pixels.length; i++) {
            int R = bytes[j++] & 0xff;
            int G = bytes[j++] & 0xff;
            int B = bytes[j++] & 0xff;
            int A = bytes[j++] & 0xff;

            int pixel = (A << 24) | (B << 16) | (G << 8) | R;
            pixels[i] = pixel;
        }


        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;

    }

    public static byte[] YUV420toNV21(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
//        Log.d("abhish", " Width is" + width + "and height is " + height);

        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
                    break;
            }

            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();

            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
//        Log.d("abhi","inside byte array");
        return data;
    }

    public static void decodeYUV(int[] out, byte[] fg, int width, int height)
            throws NullPointerException, IllegalArgumentException {
        int sz = width * height;
        if (out == null)
            throw new NullPointerException("buffer out is null");
        if (out.length < sz)
            throw new IllegalArgumentException("buffer out size " + out.length
                    + " < minimum " + sz);
        if (fg == null)
            throw new NullPointerException("buffer 'fg' is null");
        if (fg.length < sz)
            throw new IllegalArgumentException("buffer fg size " + fg.length
                    + " < minimum " + sz * 3 / 2);
        int i, j;
        int Y, Cr = 0, Cb = 0;
        for (j = 0; j < height; j++) {
            int pixPtr = j * width;
            final int jDiv2 = j >> 1;
            for (i = 0; i < width; i++) {
                Y = fg[pixPtr];
                if (Y < 0)
                    Y += 255;
                if ((i & 0x1) != 1) {
                    final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
                    Cb = fg[cOff];
                    if (Cb < 0)
                        Cb += 127;
                    else
                        Cb -= 128;
                    Cr = fg[cOff + 1];
                    if (Cr < 0)
                        Cr += 127;
                    else
                        Cr -= 128;
                }
                int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
                if (R < 0)
                    R = 0;
                else if (R > 255)
                    R = 255;
                int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1)
                        + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
                if (G < 0)
                    G = 0;
                else if (G > 255)
                    G = 255;
                int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
                if (B < 0)
                    B = 0;
                else if (B > 255)
                    B = 255;

                out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
            }
        }

    }

    public static Bitmap RGBA2ARGB(Bitmap img)
    {

        int width  = img.getWidth();
        int height = img.getHeight();

        int[] pixelsIn  = new int[height*width];
        int[] pixelsOut = new int[height*width];

        img.getPixels(pixelsIn,0,width,0,0,width,height);

        int pixel=0;
        int count=width*height;

        while(count-->0){
            int inVal = pixelsIn[pixel];

            //Get and set the pixel channel values from/to int  //TODO OPTIMIZE!
            int r = (int)( (inVal & 0xff000000)>>24 );
            int g = (int)( (inVal & 0x00ff0000)>>16 );
            int b = (int)( (inVal & 0x0000ff00)>>8  );
            int a = (int)(  inVal & 0x000000ff)      ;

            pixelsOut[pixel] = (int)( a <<24 | r << 16 | g << 8 | b );
            pixel++;
        }

        Bitmap out =  Bitmap.createBitmap(pixelsOut,0,width,width,height, Bitmap.Config.ARGB_8888);
        return out;
    }

    public static Bitmap yuv420ToBitmap(Context context,byte[] bytes) {
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicYuvToRGB script = ScriptIntrinsicYuvToRGB.create(
                rs, Element.U8_4(rs));

        // Refer the logic in a section below on how to convert a YUV_420_888 image
        // to single channel flat 1D array. For sake of this example I'll abstract it
        // as a method.
//        byte[] yuvByteArray = yuv420ToByteArray(image);
//        byte[] yuvByteArray = bytes;

//        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs))
//                .setX(yuvByteArray.length);
        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs))
                .setX(bytes.length);
        Allocation in = Allocation.createTyped(
                rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(1920)
                .setY(1080);
        Allocation out = Allocation.createTyped(
                rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        // The allocations above "should" be cached if you are going to perform
        // repeated conversion of YUV_420_888 to Bitmap.
        in.copyFrom(bytes);
        script.setInput(in);
        script.forEach(out);

        Bitmap bitmap = Bitmap.createBitmap(
                1920, 1080, Bitmap.Config.ARGB_8888);
        out.copyTo(bitmap);
        return bitmap;
    }


}
