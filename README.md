SubMerger
====================================
**Subtitle merging utility.**
**Written in Kotlin and uses Compose Multiplatform for UI.**

Usage
-
1. Import an SSA file that has styles. (e.g. the default 'format.ass' file that comes with the utility)
2. Create your sets of subtitles and add files to each of them.
    > Supported subtitle file formats: **.srt .ssa .ass**
3. Assign the styles you imported to subtitle sets.
4. Choose an output folder.
5. *(Optional) Add individual offsets in milliseconds to subtitle sets.*
*All dialogue lines in every file of the set will be offset by that value.*
6. *(Optional) Choose a set to synchronize all other sets to.* 
*Small differences in timings can be very distracting.* 
*You can also change the threshold for detecting the lines that should be shown at the same time*
7. Click 'Convert'.
8. Enjoy watching

> By clicking + on a set you can add a "blank" entry.
> Subtitle files are matched by rows so using blank entries allows you to pad your sets. 

Intent
-
Originally started as a personal Python CLI utility for merging Japanese and
English subtitles together and display the English lines in the corner.

This allowed for easier Japanese learning experience where I
looked at the translation only if there was a pressing need to.

That utility had few major pitfalls:

- Having to make all the subtitle files have matching names.
- Being limited to only 2 subtitle sets.
- Lack of GUI.
- Bugs.

So now I've rewritten it in Kotlin with Compose for UI.