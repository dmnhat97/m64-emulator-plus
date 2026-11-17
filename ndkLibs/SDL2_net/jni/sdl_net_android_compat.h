/*
 * SDL_net is built with -DWITHOUT_SDL for this project (no link to libSDL2).
 * Provide minimal stdlib/SDL_stdinc-style macros expected by SDL_net sources.
 */
#ifndef SDL_NET_ANDROID_COMPAT_H
#define SDL_NET_ANDROID_COMPAT_H

#ifndef WITHOUT_SDL
#define WITHOUT_SDL 1
#endif

/* SDL_net.h uses SDL_VERSIONNUM when SDL is not linked. */
#ifndef SDL_VERSIONNUM
#define SDL_VERSIONNUM(X, Y, Z) ((X)*1000000 + (Y)*1000 + (Z))
#endif

#define SDL_malloc  malloc
#define SDL_free    free
#define SDL_realloc realloc
#define SDL_calloc  calloc
#define SDL_memset  memset
#define SDL_memcpy  memcpy
#define SDL_vsnprintf vsnprintf

#endif /* SDL_NET_ANDROID_COMPAT_H */
