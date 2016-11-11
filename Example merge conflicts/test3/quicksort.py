<<<<<<< HEAD
def switch(myList, one, two):
    temp=myList[one]
    myList[one]=myList[two]
    myList[two]=temp

def sortMyList(myList):
    newList = myList.copy()
    quicksort(newList, 0, len(newList))
    return newList

def divide(myList, start, end):
=======
def quicksort(myList, start, end):
    if start < end:
        # partition the list
        pivot = partition(myList, start, end, start+1, end)
        # sort both halves
        quicksort(myList, start, pivot-1)
        quicksort(myList, pivot+1, end)

def partition(myList, start, end, left, right):
>>>>>>> 8349eb95993769a91211d766ee046915c1e5a272
    pivot = myList[start]
    done = False
    while not done:
        while left <= right and myList[left] <= pivot:
            left = left + 1
        while myList[right] >= pivot and right >=left:
            right = right -1
        if right < left:
            done= True
        else:
            # swap places
<<<<<<< HEAD
            switch(myList, left, right)
    # swap start with myList[right]
    switch(myList, start, right)
    return right

def quicksort(myList, start, end):
    if start < end:
        # partition the list
        pivot = divide(myList, start, end)
        # sort both halves
        conquer(myList, start, pivot-1)
        conquer(myList, pivot+1, end)
=======
            swap(myList, left, right)
    # swap start with myList[right]
    swap(myList, start, right)
    return right

def swap(myList, one, two):
    myList[one]=myList[two]
    myList[two]=myList[one]

>>>>>>> 8349eb95993769a91211d766ee046915c1e5a272

def main():
    myList = [3, 2, 9, 0, 7, 3, 5, 12]
    myList = quicksort(myList, 0, len(myList)-1)
    print myList

main()
