---
layout: post
title:  "What is Git Reset?"
date:   2016-08-02 1:50:00 -0500
author: Eric Walker
categories: jekyll update
---

Oh no. I just committed a file containing my username, password, social 
security number, and mother’s maiden name. How do I make sure this never 
sees the light of day? Stack overflow would probably tell you “just do a 
`git reset --hard`”. But what does `git reset` do?

The documentation page for reset actually lists the ability to erase 
commits as the last of three usages for reset. These three options are:

{% highlight git %}git reset [-q] [<tree-ish>] [--] <paths>…​
git reset (--patch | -p) [<tree-ish>] [--] [<paths>…​]
git reset [--soft | --mixed [-N] | --hard | --merge | --keep] [-q] [<commit>]{% endhighlight %}

The first is the way to undo `git add`. If you added some file, text.txt, 
that you really didn’t want to add, then typing in `git reset HEAD text.txt` 
(or `git reset -- text.txt`) will remove text.txt from the index (where 
it got put when you typed `git add text.txt`, or did something more complicated 
with the same effect). The `-q` is for quiet, which would make Git only output 
errors. 

The next option is if you want to have a little more control over 
what happens, so you could unstage just the password you saved, but not 
the social security number that you want the world to know. 

Finally, option 3 is for undoing entire commits, well, sort of. 
First off, let’s get the usage - if you committed changes, and you just 
want to go back 1 commit, you do a `git reset HEAD~1`. What this really 
does is change the file `.git/HEAD` and `.git/refs/heads/<current branch>` 
to contain the 40-character hash for the parent of `HEAD` instead of the 
hash for `HEAD`. You can actually set this to any commit that you know the 
hash for, so like `git reset acce7b` will reset to the commit that starts 
with that hash. There are several modes that you can specify here, 
soft, mixed, hard, merge and keep:

1. *Soft*: only change the repository, don’t change anything in the index or 
working directory.

2. *Mixed*: (the default) change the repository and reset the files in the 
index to those stored in the commit (yes actually entirely stored in the 
commit, see the commit post).

3. *Hard*: reset everything! This throws away all changes in the index and 
working directory and replaces them with the files in the commit.

4. *Merge*: resets the index and files in the working directory to what’s 
stored in the commit, but it keeps files that have unstaged changes in 
the working directory. An error is thrown and the reset stops if a file 
is different between the current commit and the one to reset to and has 
unstaged changes. Usually used to undo a ‘merge operation’.

5. *Keep*: does a hard reset, but aborts if there are local changes in a file 
that changed between the target commit and the current commit and keeps 
uncommitted changes in the working directory and index.


___


Some useful things to know:
`Git reset ORIG_HEAD` after a pull or merge to undo it (add `--merge` to avoid losing local changes)
If you don’t specify anything, this will just un-stage changes in the index

What if I want to undo a commit way way back, but not everything in between?
The answer is `git revert`-- for another day.
