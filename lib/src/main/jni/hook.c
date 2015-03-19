/*
 * Copyright 2014-2015 Marvin Wißfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <android/log.h>
#include <sys/mman.h>
#include <errno.h>
#include <unistd.h>
#include <sys/ptrace.h>

#define LOGV(...)  ((void)__android_log_print(ANDROID_LOG_VERBOSE, "ArtHook_native", __VA_ARGS__))

JNIEXPORT void JNICALL Java_de_larma_arthook_Native_munprotect(JNIEnv *env, jclass _cls, jlong addr, jlong len) {
    int pagesize = sysconf(_SC_PAGESIZE);
    int alignment = (addr%pagesize);

    int i = mprotect((void *)(addr-alignment), (size_t)(len+alignment), PROT_READ | PROT_WRITE | PROT_EXEC);
    if (i == -1) {
        LOGV("mprotect failed: %d", errno);
    }
}

JNIEXPORT void JNICALL Java_de_larma_arthook_Native_memcpy(JNIEnv *env, jclass _cls, jlong src, jlong dest, jint length) {
    char* srcPnt = (char*)src;
    char* destPnt = (char*)dest;
    for(int i = 0; i < length; ++i) {
        destPnt[i] = srcPnt[i];
    }
}

JNIEXPORT void JNICALL Java_de_larma_arthook_Native_memput(JNIEnv *env, jclass _cls, jbyteArray src, jlong dest) {
    jbyte *srcPnt = (*env)->GetByteArrayElements(env, src, 0);
    jsize length = (*env)->GetArrayLength(env, src);
    unsigned char* destPnt = (unsigned char*)dest;
    for(int i = 0; i < length; ++i) {
        destPnt[i] = srcPnt[i];
    }
    (*env)->ReleaseByteArrayElements(env, src, srcPnt, 0);
}

JNIEXPORT jbyteArray JNICALL Java_de_larma_arthook_Native_memget(JNIEnv *env, jclass _cls, jlong src, jint length) {
    jbyteArray dest = (*env)->NewByteArray(env, length);
    if (dest == NULL) {
        return NULL;
    }
    unsigned char *destPnt = (unsigned char*)(*env)->GetByteArrayElements(env, dest, 0);
    unsigned char *srcPnt = (unsigned char*)src;
    for(int i = 0; i < length; ++i) {
        destPnt[i] = srcPnt[i];
    }
    (*env)->ReleaseByteArrayElements(env, dest, destPnt, 0);
    return dest;
}

JNIEXPORT jlong JNICALL Java_de_larma_arthook_Native_mmap(JNIEnv *env, jclass _cls, jint length) {
    unsigned char *space = mmap(0, length, PROT_READ|PROT_WRITE|PROT_EXEC, MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
    if (space == MAP_FAILED) {
        LOGV("mmap failed: %d",errno);
        return 0;
    }
    return (jlong) space;
}

JNIEXPORT void JNICALL Java_de_larma_arthook_Native_munmap(JNIEnv *env, jclass _cls, jlong addr, jint length) {
    int r = munmap((void*)addr, length);
    if (r == -1) {
        LOGV("munmap failed: %d",errno);
    }
}

JNIEXPORT void JNICALL Java_de_larma_arthook_Native_ptrace(JNIEnv* env, jclass _cls, jint pid) {
    ptrace(PTRACE_ATTACH,(pid_t)pid,0,0);
}