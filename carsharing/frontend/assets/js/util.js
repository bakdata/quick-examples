export default class util {

    static getGreenToRed(percent) {
        let r = percent < 50 ? 255 : Math.floor(255 - (percent * 2 - 100) * 255 / 100);
        let g = percent > 50 ? 255 : Math.floor((percent * 2) * 255 / 100);
        return [r, g, 0, 200]
    }

    static insert(element, index, array) {
        array.splice(index + 1, 0, element);
        return array;
    }

    static locationOf(element, array, start, end) {
        start = start || 0;
        end = end || array.length;
        let pivot = parseInt(start + (end - start) / 2, 10);
        if (array[pivot] === element) return pivot;
        if (end - start <= 1)
            return array[pivot] > element ? pivot - 1 : pivot;
        if (array[pivot] < element) {
            return this.locationOf(element, array, pivot, end);
        } else {
            return this.locationOf(element, array, start, pivot);
        }
    }

    static openInNewTab(url) {
        const win = window.open(url, '_blank');
        win.focus();
    }
}
