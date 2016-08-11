---
layout: post
title:  "What is Git Revert?"
date:   2016-08-04 1:50:00 -0500
author: Eric Walker
categories: jekyll update
---

Great. Some genius decided it would be fun to put in 300 useless changes, 
but it’s 72 commits ago. You can’t do a reset because you need those 72 
commits, but you want to undo those changes, and not by hand. Enter `git revert`.

`Git revert` is relatively limited in the scope of things it does, 
which is nice, so here’s how to use the command line version of it:

{% highlight git %}git revert [--[no-]edit] [-n] [-m parent-number] [-s] [-S[<keyid>]] <commit>…​
git revert --continue
git revert --quit
git revert --abort{% endhighlight %}

Basically, git revert takes your current files, and deletes any changes 
from the commit(s) you give it, making a new commit. Options you have are:

1. `-e/--edit`: edit the commit message before committing the revert 
(this is default, add the no- between dashes and ‘edit’ to make it not ask you to write a message)
2. `-m parent-number/--mainline parent-number`: this lets you revert a 
merge (which you usually can’t do because Git has no way of knowing which 
parent was the one you want). However note that this makes it so that 
future merges will not include any changes that are part of a commit 
that was an ancestor to the reverted merge. For a complete explanation 
of reverts and how to use them in this case, see https://github.com/git/git/blob/master/Documentation/howto/revert-a-faulty-merge.txt
3. `-n/--no-commit`: if you don’t want a commit to be automatically 
created by the revert, then use this. This is useful if you’re reverting 
more than one commit in a row, then you can do it all at once
4. `-S[<keyid>]/--gpg-sign[=<keyid>]`: the keyid argument is option and 
defaults to the committer identity, but it has to be the no space option. 
The GPG thing is just who is the author
5. `-s/--signoff`: add a signoff line at the end of the automatically 
created message, this is explained in detail surrounding commit
6. '--strategy=<strategy>': use the given merge strategy (should only be used once), 
this is the same strategy thing that merge uses
7. `-X<option>/--stategy-option=<option>`: pass the merge strategy-specific 
option to the merge strategy, this is explained more with merge

___

Some basic examples: `git revert HEAD~1`: revert the changes of the parent of HEAD and create a new commit.

`git revert -n branch~4..branch`: revert all changes done by the last 5 
commits of the branch ‘branch’, but don’t make a commit, just put changes 
in the index and working directory
