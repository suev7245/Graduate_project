package com.example.sm_pc.trial1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

public class AvcEncoder extends Thread
{
    private final static String TAG = AvcEncoder.class.getSimpleName();
    private final static String MIME_TYPE = "video/avc";
    private final static int I_FRAME_INTERVAL = 1;

    MediaCodec codec;
    MediaFormat mediaFormat;
    int width;
    int height;
    int timeoutUSec = 10000;
    int frameIndex = 0;
    byte[] spsPpsInfo = null;
    byte[] yuv420 = null;
    int frameRate;
    int yStride;
    int cStride;
    int ySize;
    int cSize;
    int halfWidth;
    int halfHeight;

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public AvcEncoder()
    {
    }

    public boolean init(int width, int height, int framerate, int bitrate)
    {
        try
        {
            codec = MediaCodec.createEncoderByType(MIME_TYPE);
        }
        catch (IOException e)
        {
            return false;
        }

        boolean isSupport = false;
        int colorFormat = 0;
        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(MIME_TYPE);
        for (int i = 0; i < capabilities.colorFormats.length && colorFormat == 0; i++)
        {
            int format = capabilities.colorFormats[i];
            if (format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            {
                isSupport = true;
                break;
            }
        }
        if (!isSupport)
            return false;

        this.width  = width;
        this.height = height;
        this.halfWidth = width / 2;
        this.halfHeight = height / 2;
        this.frameRate = framerate;

        this.yStride = (int) Math.ceil(width/16.0f) * 16;
        this.cStride = (int) Math.ceil(width/32.0f)  * 16;
        this.ySize = yStride * height;
        this.cSize = cStride * height / 2;

        this.yuv420 = new byte[width*height*3/2];

        mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);

//        MediaFormat outputFormat = codec.getOutputFormat(); // option B

        codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        codec.start();
        return true;
    }

    public void close()
    {
        try
        {
            codec.stop();
            codec.release();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void inputEncoder(byte[] input) {
        NV21toYUV420PackedSemiPlanar(input, yuv420, width, height);
        try {
            ByteBuffer[] inputBuffers = codec.getInputBuffers();
            int inputBufferIndex = codec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                if (inputBuffer.capacity() < yuv420.length) {
                    byte[] temp = new byte[yuv420.length];
                    System.arraycopy(yuv420, 0, temp, 0, temp.length);
                    inputBuffer.put(temp);
                } else {
                    inputBuffer.put(input);
                }
                codec.queueInputBuffer(inputBufferIndex, 0, yuv420.length,
                        0, 0);
            }

        } catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    public void run(){
        try {
            ByteBuffer[] outputBuffers = codec.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUSec);


//            if (outputBufferIndex <0) {
//                switch (outputBufferIndex) {
//                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
//                        Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
//                        codec.getOutputBuffers();
//                        break;
//
//                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
//                        Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " + codec.getOutputFormat());
//                        break;
//
//                    case MediaCodec.INFO_TRY_AGAIN_LATER:
//                        //Log.d(TAG, "INFO_TRY_AGAIN_LATER");
//                        break;
//                    default:
//                        ;
//
//                }
//                Thread.sleep(500);
//
//            }
//            else{
            while(true){
                while(outputBufferIndex<0){
                    Thread.sleep(500);
                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUSec);
                }

                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);

                if (spsPpsInfo == null)
                {
                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
                    if (spsPpsBuffer.getInt() == 0x00000001)
                    {
                        spsPpsInfo = new byte[outData.length];
                        System.arraycopy(outData, 0, spsPpsInfo, 0, outData.length);
                    }
                    else
                    {
                        break;
                    }
                }
                else
                {
                    outputStream.write(outData);
                }
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUSec);
            }
            codec.releaseOutputBuffer(outputBufferIndex, false);
//            }
        }catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

//    public byte[] outputEncoder(){
//        try {
//            ByteBuffer[] outputBuffers = codec.getOutputBuffers();
//
//            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//            int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUSec);
////            int outputBufferIndex = 0;
//
//            switch (outputBufferIndex) {
//                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
//                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
//                    codec.getOutputBuffers();
//                    break;
//
//                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
//                    Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " + codec.getOutputFormat());
//                    break;
//
//                case MediaCodec.INFO_TRY_AGAIN_LATER:
//                    //Log.d(TAG, "INFO_TRY_AGAIN_LATER");
//                    break;
//
//                default:
//                    ;
//
//            }
//            while (outputBufferIndex >= 0)
//            {
//                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
//                byte[] outData = new byte[bufferInfo.size];
//                outputBuffer.get(outData);
//
//                if (spsPpsInfo == null)
//                {
//                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
//                    if (spsPpsBuffer.getInt() == 0x00000001)
//                    {
//                        spsPpsInfo = new byte[outData.length];
//                        System.arraycopy(outData, 0, spsPpsInfo, 0, outData.length);
//                    }
//                    else
//                    {
//                        return null;
//                    }
//                }
//                else
//                {
//                    outputStream.write(outData);
//                }
//
//                codec.releaseOutputBuffer(outputBufferIndex, false);
//                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUSec);
//            }
//
//            byte[] ret = outputStream.toByteArray();
//            if (ret.length > 5 && ret[4] == 0x65) //key frame need to add sps pps
//            {
//                outputStream.reset();
//                outputStream.write(spsPpsInfo);
//                outputStream.write(ret);
//            }
//
//        }
//        catch (Throwable t)
//        {
//            t.printStackTrace();
//        }
//        byte[] ret = outputStream.toByteArray();
//        outputStream.reset();
//        return ret;
//    }

//    public byte[] offerEncoder(byte[] input)
//    {
//        NV21toYUV420PackedSemiPlanar(input, yuv420, width, height);
//        try {
//            ByteBuffer[] inputBuffers = codec.getInputBuffers();
//            ByteBuffer[] outputBuffers = codec.getOutputBuffers();
//            int inputBufferIndex = codec.dequeueInputBuffer(-1);
//            if (inputBufferIndex >= 0)
//            {
////                long pts = computePresentationTime(frameIndex, frameRate);
////                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
////                inputBuffer.clear();
////                inputBuffer.put(yuv420, 0, yuv420.length);
////                codec.queueInputBuffer(inputBufferIndex, 0, yuv420.length, pts, 0);
////                frameIndex++;
//                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
//                inputBuffer.clear();
//                if (inputBuffer.capacity() < yuv420.length) {
//                    byte[] temp = new byte[yuv420.length];
//                    System.arraycopy(yuv420, 0, temp, 0, temp.length);
//                    inputBuffer.put(temp);
//                } else {
//                    inputBuffer.put(input);
//                }
//                codec.queueInputBuffer(inputBufferIndex, 0, yuv420.length,
//                        0, 0);
//            }
//
//            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//            int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUSec);
////            int outputBufferIndex = 0;
//
//            switch (outputBufferIndex) {
//                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
//                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
//                    codec.getOutputBuffers();
//                    break;
//
//                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
//                    Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " + codec.getOutputFormat());
//                    break;
//
//                case MediaCodec.INFO_TRY_AGAIN_LATER:
//                    //Log.d(TAG, "INFO_TRY_AGAIN_LATER");
//                    break;
//
//                default:
//                    ;
//
//            }
//            while (outputBufferIndex >= 0)
//            {
//                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
//                byte[] outData = new byte[bufferInfo.size];
//                outputBuffer.get(outData);
//
//                if (spsPpsInfo == null)
//                {
//                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
//                    if (spsPpsBuffer.getInt() == 0x00000001)
//                    {
//                        spsPpsInfo = new byte[outData.length];
//                        System.arraycopy(outData, 0, spsPpsInfo, 0, outData.length);
//                    }
//                    else
//                    {
//                        return null;
//                    }
//                }
//                else
//                {
//                    outputStream.write(outData);
//                }
//
//                codec.releaseOutputBuffer(outputBufferIndex, false);
//                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUSec);
//            }
//
//            byte[] ret = outputStream.toByteArray();
//            if (ret.length > 5 && ret[4] == 0x65) //key frame need to add sps pps
//            {
//                outputStream.reset();
//                outputStream.write(spsPpsInfo);
//                outputStream.write(ret);
//            }
//
//        }
//        catch (Throwable t)
//        {
//            t.printStackTrace();
//        }
//        byte[] ret = outputStream.toByteArray();
//        outputStream.reset();
//        return ret;
//    }

    public static byte[] NV21toYUV420PackedSemiPlanar(final byte[] input, final byte[] output, final int width, final int height) {
        /*
         * COLOR_TI_FormatYUV420PackedSemiPlanar is NV12.(plane[0]->Y, plane[1]->UV)
         * So we just have to reverse U and V.
         */
        final int frameSize = width * height;
        final int qFrameSize = frameSize/4;

        System.arraycopy(input, 0, output, 0, frameSize); // Y

        for (int i = 0; i < qFrameSize; i++) {
            output[frameSize + i*2] = input[frameSize + i*2 + 1]; // Cb (U)
            output[frameSize + i*2 + 1] = input[frameSize + i*2]; // Cr (V)
        }
        return output;
    }

    public byte[] YV12toYUV420PackedSemiPlanar(final byte[] input, final byte[] output, final int width, final int height)
    {
        for (int i=0; i<height; i++)
            System.arraycopy(input, yStride*i, output, yStride*i, yStride); // Y

        for (int i=0; i<halfHeight; i++)
        {
            for (int j=0; j<halfWidth; j++)
            {
                output[ySize + (i*halfWidth + j)*2] = input[ySize + cSize + i*cStride + j]; // Cb (U)
                output[ySize + (i*halfWidth + j)*2 + 1] = input[ySize + i*cStride + j]; // Cr (V)
            }
        }
        return output;
    }


    private static MediaCodecInfo selectCodec(String mimeType)
    {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++)
        {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder())
                continue;

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++)
            {
                if (types[j].equalsIgnoreCase(mimeType))
                    return codecInfo;
            }
        }
        return null;
    }

    private long computePresentationTime(long frameIndex, int framerate)
    {
        return 132 + frameIndex * 1000000 / framerate;
    }
}