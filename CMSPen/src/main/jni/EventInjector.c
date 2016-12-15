#include <string.h>
#include <stdint.h>
#include <jni.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <dirent.h>
#include <time.h>
#include <errno.h>

#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/inotify.h>
#include <sys/limits.h>
#include <sys/poll.h>

#include <linux/fb.h>
#include <linux/kd.h>
#include <linux/input.h>

#include <android/log.h>
#define TAG "CMSPen::JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG, __VA_ARGS__) 
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
//#define EVENT_SIZE  ( sizeof (struct inotify_event) )
//#define BUF_LEN     ( 1024 * ( EVENT_SIZE + 16 ) )



/* Debug tools
 */
 int g_debug = 0;


jint Java_com_tushar_cmspen2_libsuperuser_Events_intEnableDebug( JNIEnv* env,jobject thiz, jint enable ) {

	g_debug = enable;
	return g_debug;
}
 
/*jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	return JNI_VERSION_1_2; //1_2 1_4
}

void JNI_OnUnload(JavaVM *vm, void *reserved)
{

}*/

static struct typedev {
	struct pollfd ufds;
	char *device_path;
	char *device_name;
} *pDevs = NULL;
struct pollfd *ufds;
//struct pollfd *devfds;
static int nDevsCount;

const char *device_path = "/dev/input";

int g_Polling = 0;
struct input_event event;
int c;
int i;
int pollres;
int get_time = 0;
char *newline = "\n";
uint16_t get_switch = 0;
struct inotify_event notify_event;
int version;

int dont_block = -1;
int event_count = 0;
int sync_rate = 0;
int64_t last_sync_time = 0;
const char *device = NULL;
struct pollfd *devfds;


static int open_device(int index)
{
	if (index >= nDevsCount || pDevs == NULL) return -1;
	char *device = pDevs[index].device_path;
	
    int version;
    int fd;
    
    char name[80];
    char location[80];
    char idstr[80];
    struct input_id id;
	
    fd = open(device, O_RDWR);
    if(fd < 0) {
		pDevs[index].ufds.fd = -1;
		
		pDevs[index].device_name = NULL;
        return -1;
    }
    
	pDevs[index].ufds.fd = fd;
	ufds[index].fd = fd;
	
    name[sizeof(name) - 1] = '\0';
    if(ioctl(fd, EVIOCGNAME(sizeof(name) - 1), &name) < 1) {
        name[0] = '\0';
    }
	
	pDevs[index].device_name = strdup(name);
    
    
    return 0;
}

int remove_device(int index)
{
	if (index >= nDevsCount || pDevs == NULL ) return -1;
	
	int count = nDevsCount - index - 1;
	free(pDevs[index].device_path);
	free(pDevs[index].device_name);
	
	memmove(&pDevs[index], &pDevs[index+1], sizeof(pDevs[0]) * count);
	nDevsCount--;
	return 0;
} 



static int scan_dir(const char *dirname)
{
	nDevsCount = 0;
    char devname[PATH_MAX];
    char *filename;
    DIR *dir;
    struct dirent *de;
    dir = opendir(dirname);
    if(dir == NULL)
        return -1;
    strcpy(devname, dirname);
    filename = devname + strlen(devname);
    *filename++ = '/';
    while((de = readdir(dir))) {
        if(de->d_name[0] == '.' &&
           (de->d_name[1] == '\0' ||
            (de->d_name[1] == '.' && de->d_name[2] == '\0')))
            continue;
        strcpy(filename, de->d_name);
		// add new filename to our structure: devname
		struct typedev *new_pDevs = realloc(pDevs, sizeof(pDevs[0]) * (nDevsCount + 1));
		if(new_pDevs == NULL) {
			return -1;
		}
		pDevs = new_pDevs;
		
		struct pollfd *new_ufds = realloc(ufds, sizeof(ufds[0]) * (nDevsCount + 1));
		if(new_ufds == NULL) {
			return -1;
		}
		ufds = new_ufds; 
		ufds[nDevsCount].events = POLLIN;
		
		pDevs[nDevsCount].ufds.events = POLLIN;
		pDevs[nDevsCount].device_path = strdup(devname);
		
        nDevsCount++;
    }
    closedir(dir);
    return 0;
}



jint Java_com_tushar_cmspen2_libsuperuser_Events_ScanFiles( JNIEnv* env,jobject thiz ) {
	int res = scan_dir(device_path);
	if(res < 0) {
		return -1;
	}
	
	return nDevsCount;
}

jstring Java_com_tushar_cmspen2_libsuperuser_Events_getDevPath( JNIEnv* env,jobject thiz, jint index) {
	return (*env)->NewStringUTF(env, pDevs[index].device_path);
}
jstring Java_com_tushar_cmspen2_libsuperuser_Events_getDevName( JNIEnv* env,jobject thiz, jint index) {
	if (pDevs[index].device_name == NULL) return NULL;
	else return (*env)->NewStringUTF(env, pDevs[index].device_name);
}

jint Java_com_tushar_cmspen2_libsuperuser_Events_OpenDev( JNIEnv* env,jobject thiz, jint index ) {
	return open_device(index);
}

jint Java_com_tushar_cmspen2_libsuperuser_Events_RemoveDev( JNIEnv* env,jobject thiz, jint index ) {
	return remove_device(index);
}

jint Java_com_tushar_cmspen2_libsuperuser_Events_PollDev( JNIEnv* env,jobject thiz, jint index ) {
	if (index >= nDevsCount || pDevs[index].ufds.fd == -1) return -1;
	int pollres = poll(ufds, nDevsCount, -1);
	if(ufds[index].revents) {
		if(ufds[index].revents & POLLIN) {
			int res = read(ufds[index].fd, &event, sizeof(event));
			if(res < (int)sizeof(event)) {
				return 1;
			} 
			else return 0;
		}
	}
	return -1;
}

//Added by Tushar Dudani

jint Java_com_tushar_cmspen2_SPenDetection_openFromPath( JNIEnv* env, jobject thiz, jstring path)
{
    const char* strChars = (*env)->GetStringUTFChars(env, path, (jboolean *)0);

    int fd = open(strChars, O_RDWR);

    (*env)->ReleaseStringUTFChars(env, path, strChars);

    devfds = malloc(sizeof(struct pollfd));
    devfds[0].events = POLLIN;

    if(fd < 0)
        return -1;
    else
        return fd;
}

jint Java_com_tushar_cmspen2_SPenDetection_PollDevFD( JNIEnv* env,jobject thiz, jint fd ) {

    devfds[0].fd = fd;

    int pollres = poll(devfds, 1, -1);
    if(devfds[0].revents) {
        if(devfds[0].revents & POLLIN) {
            int res = read(devfds[0].fd, &event, sizeof(event));
            if(res < (int)sizeof(event)) {
                return 1;
            }
            else return 0;
        }
    }
    return -1;
}

jint Java_com_tushar_cmspen2_SPenDetection_BlockStart( JNIEnv* env, jobject thiz, jstring path) {
	int block_open = -1;
	int block_result = -1;

    const char* strChars = (*env)->GetStringUTFChars(env, path, (jboolean *)0);

	block_open = open(strChars, O_RDONLY);
	if (block_open == -1)
	{
		LOGD("Open failed.");
		return -1;
	}

    (*env)->ReleaseStringUTFChars(env, path, strChars);
	
	block_result = ioctl(block_open, EVIOCGRAB, 1);
	if(block_result != 0)
	{
		LOGD("Exclusive access failed.");
		return -1;
	}
	
	return block_open;
}

jint Java_com_tushar_cmspen2_SPenDetection_BlockStop( JNIEnv* env, jobject thiz, jint fd ) {
	
	ioctl(fd, EVIOCGRAB, 0);
	close(fd);
	return 0;
}

jint Java_com_tushar_cmspen2_SPenDetection_AddFileChangeListener( JNIEnv* env, jobject thiz, jstring path) {
	int fd;
	int wd;
	int length = -1;
	//struct inotify_event notify_event;
    const char* strChars = (*env)->GetStringUTFChars(env, path, (jboolean *)0);

	fd = inotify_init();
	if (fd < 0)
	{
	    LOGD("Notify failed.");
	    return -1;
	}

	if (strChars == NULL)
	{
	    LOGD("Device path null");
	    return -1;
	}
	wd = inotify_add_watch (fd, strChars, IN_ACCESS | IN_MODIFY | IN_OPEN);

    (*env)->ReleaseStringUTFChars(env, path, strChars);

    LOGD("Waiting for S Pen event....");
    while(length < 0)
    {
        length = read( fd, &notify_event, sizeof(notify_event));
        //if(length < 0)
        	//LOGD("read error.");
    }

    //LOGD("Size of event is: %d", length);
	/*if(notify_event.len == 0)
        LOGD("Length of event is 0.");
    else
    {
        if(notify_event.mask & IN_ACCESS)
            LOGD("%s was accessed.",notify_event.name);
        if(notify_event.mask & IN_MODIFY)
            LOGD("%s was modified.",notify_event.name);
        if(notify_event.mask & IN_OPEN)
            LOGD("%s was opened.",notify_event.name);
    }*/

	jclass cls = (*env)->FindClass(env, "com/tushar/cmspen2/SPenDetection");
	if(cls == 0)
    {
        LOGD("Class not found!");
        return -1;
    }
	jmethodID mID = (*env)->GetStaticMethodID(env, cls, "waitForEvent", "()V");
    if(mID == 0)
    {
        LOGD("Method not found!");
        return -1;
    }
	LOGD("Calling method waitForEvent()");
	(*env)->CallStaticVoidMethod(env, thiz, mID);
	
	( void ) inotify_rm_watch( fd, wd );
	( void ) close( fd );
	return 0;
}



//end

jint Java_com_tushar_cmspen2_libsuperuser_Events_getType( JNIEnv* env,jobject thiz ) {
	return event.type;
}

jint Java_com_tushar_cmspen2_libsuperuser_Events_getCode( JNIEnv* env,jobject thiz ) {
	return event.code;
}

jint Java_com_tushar_cmspen2_libsuperuser_Events_getValue( JNIEnv* env,jobject thiz ) {
	return event.value;
}