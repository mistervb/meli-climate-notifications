import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'localDate',
  standalone: true
})
export class LocalDatePipe implements PipeTransform {
  transform(utcDate: string | Date, format: string = 'dd/MM/yyyy HH:mm'): string {
    try {
      console.log('Original date value:', utcDate);
      console.log('Type of date value:', typeof utcDate);
      
      if (!utcDate) {
        console.log('No date provided');
        return '';
      }
      
      const date = new Date(utcDate);
      console.log('Parsed date object:', date);
      console.log('Date string:', date.toString());
      console.log('UTC string:', date.toUTCString());
      console.log('Local string:', date.toLocaleString());
      
      if (isNaN(date.getTime())) {
        console.error('Invalid date');
        return 'Invalid date';
      }
      
      // Get local date components
      const day = date.getDate().toString().padStart(2, '0');
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const year = date.getFullYear();
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      
      // Replace format placeholders with actual values
      const result = format
        .replace('dd', day)
        .replace('MM', month)
        .replace('yyyy', year.toString())
        .replace('HH', hours)
        .replace('mm', minutes);
      
      console.log('Formatted result:', result);
      return result;
    } catch (error) {
      console.error('Error formatting date:', error);
      return 'Date error';
    }
  }
}
