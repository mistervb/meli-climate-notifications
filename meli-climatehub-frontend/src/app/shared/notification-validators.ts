import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export class NotificationValidators {
  static futureDate(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      const selectedDate = new Date(control.value);
      const now = new Date();

      if (selectedDate <= now) {
        return { futureDate: true };
      }

      return null;
    };
  }

  static endDateAfterExecuteAt(executeAtControlName: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      const endDate = new Date(control.value);
      const executeAtControl = control.root.get(executeAtControlName);

      if (!executeAtControl || !executeAtControl.value) {
        return null;
      }

      const executeAtDate = new Date(executeAtControl.value);

      if (endDate <= executeAtDate) {
        return { endDateAfterExecuteAt: true };
      }

      return null;
    };
  }

  static validTimeFormat(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      // Se for um objeto Date, extrair apenas o horário
      if (control.value instanceof Date) {
        const hours = control.value.getHours();
        const minutes = control.value.getMinutes();
        const timeString = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;
        
        // Validar formato HH:mm
        if (!/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(timeString)) {
          return { invalidTimeFormat: true };
        }

        // Validar intervalo permitido (06:00 - 22:00)
        const time = new Date();
        time.setHours(hours, minutes, 0, 0);
        const minTime = new Date();
        minTime.setHours(6, 0, 0, 0);
        const maxTime = new Date();
        maxTime.setHours(22, 0, 0, 0);

        if (time < minTime || time > maxTime) {
          return { invalidTimeRange: true };
        }

        return null;
      }

      // Se for uma string, validar diretamente
      if (typeof control.value === 'string') {
        if (!/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(control.value)) {
          return { invalidTimeFormat: true };
        }

        const [hours, minutes] = control.value.split(':').map(Number);
        const time = new Date();
        time.setHours(hours, minutes, 0, 0);
        const minTime = new Date();
        minTime.setHours(6, 0, 0, 0);
        const maxTime = new Date();
        maxTime.setHours(22, 0, 0, 0);

        if (time < minTime || time > maxTime) {
          return { invalidTimeRange: true };
        }
      }

      return null;
    };
  }

  static validDayOfWeek(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      const day = Number(control.value);
      if (isNaN(day) || day < 1 || day > 7) {
        return { invalidDayOfWeek: true };
      }

      return null;
    };
  }

  static validLocation(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      // Validar cidade (apenas letras, espaços e hífens)
      if (control.get('cityName')?.value && !/^[a-zA-ZÀ-ÿ\s\-]+$/.test(control.get('cityName')?.value)) {
        return { invalidCity: true };
      }

      // Validar UF (2 letras maiúsculas)
      if (control.get('uf')?.value && !/^[A-Z]{2}$/.test(control.get('uf')?.value)) {
        return { invalidUf: true };
      }

      return null;
    };
  }

  static noOverlappingSchedule(existingSchedules: any[]): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value || !control.value.time || !control.value.cityName || !control.value.uf) {
        return null;
      }

      const newTime = new Date(`1970-01-01T${control.value.time}`);
      const cityName = control.value.cityName;
      const uf = control.value.uf;

      // Verificar se já existe um agendamento para a mesma cidade no mesmo horário
      const hasOverlap = existingSchedules.some(schedule => {
        if (schedule.cityName === cityName && schedule.uf === uf) {
          const scheduleTime = new Date(`1970-01-01T${schedule.time}`);
          const timeDiff = Math.abs(newTime.getTime() - scheduleTime.getTime());
          // Verificar se a diferença é menor que 1 hora (3600000 milissegundos)
          return timeDiff < 3600000;
        }
        return false;
      });

      if (hasOverlap) {
        return { overlappingSchedule: true };
      }

      return null;
    };
  }

  static maxActiveSchedules(maxSchedules: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const activeSchedules = control.value?.filter((schedule: any) => schedule.status === 'ACTIVE');
      if (activeSchedules && activeSchedules.length >= maxSchedules) {
        return { maxActiveSchedulesExceeded: true };
      }
      return null;
    };
  }
} 