---
layout: post
title:  "What is Git Commit?"
date:   2016-08-12 1:50:00 -0500
author: Eric Walker
categories: jekyll update
---

Anyone who has successfully used Git in any form has probably used the 
command `git commit`. In its most basic form, with just a commit message, 
this does a few things: it stores the files in the staging area, or index, 
in a new commit with a log message from the user, which is supposed to 
describe the changes (but is more often than not “asdf”) and it updates 
the current branch to have a head at the latest commit. Simple, right?

Wait, what does any of that mean? “Storing files in the index in a commit”? 
Well, Git is all about being lightweight and not storing anything it doesn’t 
need to, so this means that Git doesn’t want to have complete copies of 
every single file for every single commit. You might ask, why not just 
store what changed? This would be correct for most other version control 
systems like CVS, Subversion, Perforce, Bazaar, etc., but Git doesn’t do this. 
Git takes “snapshots” of the files in the directory you’re tracking. 
A snapshot will just include a pointer to a previous snapshot if a particular 
file hasn’t changed, but will store new versions. If you were to look at 
the actual data of a commit, you would see the content of all the files 
that Git is tracking. This supposedly makes Git more powerful in terms 
of branching, but that’s for another day.

Anyway, Git stores a beautiful “snapshot” in the local repository, then 
it also changes the contents of two files to be the commit hash 
(the 40-character fa90c29… that you see after the commit happens). 
These files happen to be `.git/refs/heads/<current branch name>` and `.git/HEAD`, 
which are just Git’s place to store the head of the specified branch 
(note, it’s a pointer, not a full commit) and the head of the current branch.

So, Git has created a new commit and updated a branch head. What about the other things that you can do? 
Answer is: way more than you ever wanted to do or even think about. 
Commit has a number of parameters, as can be seen in the synopsis of the help page:

{% highlight git %}git commit [-a | --interactive | --patch] [-s] [-v] [-u<mode>] [--amend]
	   [--dry-run] [(-c | -C | --fixup | --squash) <commit>]
	   [-F <file> | -m <msg>] [--reset-author] [--allow-empty]
	   [--allow-empty-message] [--no-verify] [-e] [--author=<author>]
	   [--date=<date>] [--cleanup=<mode>] [--[no-]status]
	   [-i | -o] [-S[<keyid>]] [--] [<file>…​]{% endhighlight %}
	   
How do we read this? It means `git commit` can be followed by these things. Let’s take them one brace at a time:

`[-a | --interactive | --patch]`: these say how to choose which files to look at. `Git commit` 
just looks at the index, but adding -a will look at all files in the working 
directory that are also in the index, and stage changes in modified files.
 
`--interactive or --patch` will give you an interface to choose which changes to commit.

`-s/--signoff`: this will automatically add a signed-off by line by the 
committer at the end of the commit message. This typically is used to certify 
that a committer has the rights to submit the particular commit

`-v/--verbose`: this will show the diff between the current HEAD commit 
and what would be committed at the bottom of the message template that is 
generated, useful for writing commit messages so you can see what you did.

`-u[<mode>]/--untracked-files[=<mode>]`: shows the untracked files. The 
mode can be `no`: show no untracked files, `normal`: show untracked files 
in the root directory and untracked directories, and `all`: show all 
untracked files, even those in untracked directories. Usage looks like 
`git commit -uno` or `git commit --untracked-files=all`. The default when using `-u` 
is all and when not using it, the default is `normal`.

`--amend`: this one is to be used responsibly. If you ‘accidentally’ wrote “asdf” 
as your commit message, or forgot to include a file in a commit, you can 
amend the head of the current branch with this. If you do `git commit --amend`, 
command line will pull up an editor that has the previous commit message already in it, 
then once you finish writing your commit message, this new commit will replace the 
old head of the current branch. This commit will have the same parent(s) 
and authors as the current commit, but DO NOT USE if this commit has already 
been published. Rewriting history is bad.

`--dry-run`: this is for those who don’t have Git magic (e.g. me, you, everyone…) 
and want to know what the heck will happen when you press enter with a 
complicated command. For example, `git commit -a --dry-run` will show you 
all files that will be committed or not if you run the command `git commit -a`.
 
`[(-c | -C | --fixup | --squash) <commit>]`: `-C <commit>/--reuse-message=<commit>` 
will reuse the log message and authorship from `<commit>`, `-c <commit>/--reedit-message=<commit>` 
does the same thing as `-C`, but will show you the editor before actually making changes. 
`--fixup=<commit>` will make a commit that is to be used with rebase `--autosquash` 
with the message of `<commit>` prefixed by “fixup! ”. Finally `--squash=<commit>` 
does the same thing as `--fixup`, but uses the prefix “squash! “, and can be used 
with the options `-m`/`-c`/`-C`/`-F`.

`[-F <file> | -m <msg>]`: this specifies the message of the commit. `-F` 
will take the commit message from the specified files, `-m` 
will take it from the next input. For example: `git commit -m “Added 9001 files and beat Goku in single combat”` 
adds a commit with that message.

`--reset-author`: this is used with the `-C`/`-c`/`--amend` options, and will 
make the author of the commit you, rather than whoever made the commit you’re changing.

`--allow-empty`: this allows you to commit with no changes. Not recommended by me.

___

Whew… halfway! We’ll cover the rest in the next iteration of this post.
