//
//               INTEL CORPORATION PROPRIETARY INFORMATION
//  This software is supplied under the terms of a license agreement or
//  nondisclosure agreement with Intel Corporation and may not be copied
//  or disclosed except in accordance with the terms of that agreement.
//        Copyright (c) 2013-2014 Intel Corporation. All Rights Reserved.
//

package com.intel.inde.mp.android.graphics;

import android.opengl.GLES20;
import com.intel.inde.mp.domain.IFrameBuffer;
import com.intel.inde.mp.domain.Resolution;
import com.intel.inde.mp.domain.graphics.IEglUtil;

public class FrameBuffer implements IFrameBuffer {
    private int framebuffer;
    private int offScreenTexture;
    private int depthBuffer;
    private IEglUtil utils;
    private Resolution resolution;

    public FrameBuffer(IEglUtil utils) {
        this.utils = utils;
        framebuffer = -1;
        offScreenTexture = -1;
        depthBuffer = -1;
    }

    @Override
    public void setResolution(Resolution res) {
        resolution = res;
        if (framebuffer != -1) {
            release();
        }
        int[] glValues = new int[1];

        GLES20.glGenTextures(1, glValues, 0);
        utils.checkEglError("glGenTextures");
        offScreenTexture = glValues[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, offScreenTexture);
        utils.checkEglError("glBindTexture");

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, res.width(), res.height(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        utils.checkEglError("glTexImage2D");

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glGenFramebuffers(1, glValues, 0);
        utils.checkEglError("glGenFramebuffers");

        framebuffer = glValues[0];

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer);
        utils.checkEglError("glBindFramebuffer");

        GLES20.glGenRenderbuffers(1, glValues, 0);
        utils.checkEglError("glGenRenderbuffers");
        depthBuffer = glValues[0];

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthBuffer);
        utils.checkEglError("glBindRenderbuffer");

        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, res.width(), res.height());
        utils.checkEglError("glRenderbufferStorage");

        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthBuffer);
        utils.checkEglError("glFramebufferRenderbuffer");

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, offScreenTexture, 0);
        utils.checkEglError("glFramebufferTexture2D");

        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        utils.checkEglError("glCheckFramebufferStatus");



        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete, status=" + status);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        utils.checkEglError("glBindFramebuffer(0)");
    }

    @Override
    public int getTextureId() {
        return offScreenTexture;
    }

    @Override
    public void release() {
        int[] glValues = new int[1];

        if (offScreenTexture > 0) {
            glValues[0] = offScreenTexture;
            GLES20.glDeleteTextures(1, glValues, 0);
            offScreenTexture = -1;
        }

        if (framebuffer > 0) {
            glValues[0] = framebuffer;
            GLES20.glDeleteFramebuffers(1, glValues, 0);
            framebuffer = -1;
        }

        if (depthBuffer > 0) {
            glValues[0] = depthBuffer;
            GLES20.glDeleteRenderbuffers(1, glValues, 0);
            depthBuffer = -1;
        }
    }

    @Override
    public void bind() {
        GLES20.glViewport(0,0,resolution.width(), resolution.height());
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer);
    }

    @Override
    public void unbind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }
}
