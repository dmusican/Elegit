def switch(myList, one, two):
    temp=myList[one]
    myList[one]=myList[two]
    myList[two]=temp

def sortMyList(myList):
    newList = myList.copy()
    quicksort(newList, 0, len(newList))
    return newList

def divide(myList, start, end):
    pivot = myList[start]
    left = start+1
    right = end
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

def main():
    myList = [3, 2, 9, 0, 7, 3, 5, 12]
    myList = quicksort(myList, 0, len(myList)-1)
    print myList

main()
